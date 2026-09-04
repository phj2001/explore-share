package com.smartcampus.service.impl;

import com.smartcampus.dto.response.HotPoiResponse;
import com.smartcampus.dto.response.LeaderboardItemResponse;
import com.smartcampus.entity.User;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.ProfileVisibility;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.LeaderboardService;
import com.smartcampus.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final POICheckInRepository checkInRepository;
    private final POIShareRepository shareRepository;
    private final POIShareLikeRepository shareLikeRepository;
    private final UserRepository userRepository;
    private final RedisUtils redisUtils;

    private static final int CACHE_HOURS = 1;
    /** 用户维度榜单的全部缓存 key 维度：3 类型 × 2 周期（前端只传 total/week，与 getLeaderboard 的 key 构造一致） */
    private static final String[] LB_TYPES = {"checkin", "share", "likes"};
    private static final String[] LB_PERIODS = {"total", "week"};

    @Override
    public List<LeaderboardItemResponse> getLeaderboard(String type, String period, int limit) {
        String cacheKey = String.format("leaderboard:%s:%s", type, period);

        List<LeaderboardItemResponse> cached = redisUtils.getList(cacheKey, LeaderboardItemResponse.class);
        if (cached != null) {
            return cached.size() <= limit ? cached : cached.subList(0, limit);
        }

        List<Object[]> rows = switch (type) {
            case "checkin" -> "week".equals(period)
                    ? checkInRepository.countGroupedByUserIdSinceDesc(weekStart())
                    : checkInRepository.countGroupedByUserIdDesc();
            case "share" -> "week".equals(period)
                    ? shareRepository.countGroupedByUserIdSinceDesc(weekStart())
                    : shareRepository.countGroupedByUserIdDesc();
            case "likes" -> "week".equals(period)
                    ? shareLikeRepository.countReceivedLikesGroupedByUserIdSinceDesc(weekStart())
                    : shareLikeRepository.countReceivedLikesGroupedByUserIdDesc();
            default -> List.of();
        };

        List<Long> userIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .limit(limit)
                .toList();

        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<LeaderboardItemResponse> result = new ArrayList<>();
        int rank = 0;
        for (Object[] row : rows) {
            if (rank >= limit) break;
            long userId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            User user = userMap.get(userId);
            if (user == null) continue;
            // 隐私/状态过滤：非公开主页或非活跃用户不上榜（不占名次、名次连续，与 user==null 同模式），
            // 榜单属全站公开展示位，口径与 6 个公开内容端点的隐私拦截保持一致
            if (ProfileVisibility.fromCode(user.getProfileVisibility()) != ProfileVisibility.PUBLIC
                    || UserStatus.fromCode(user.getStatus()) != UserStatus.ACTIVE) {
                continue;
            }

            result.add(new LeaderboardItemResponse(
                    rank + 1,
                    userId,
                    user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    user.getAvatarUrl(),
                    count
            ));
            rank++;
        }

        redisUtils.setObject(cacheKey, result, CACHE_HOURS, TimeUnit.HOURS);
        return result;
    }

    @Override
    public List<HotPoiResponse> getHotPois(int limit) {
        String cacheKey = "leaderboard:hot-pois";

        List<HotPoiResponse> cached = redisUtils.getList(cacheKey, HotPoiResponse.class);
        if (cached != null) {
            return cached.size() <= limit ? cached : cached.subList(0, limit);
        }

        List<Object[]> rows = checkInRepository.countGroupedByPoiIdDesc();

        List<HotPoiResponse> result = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
            Object[] row = rows.get(i);
            result.add(new HotPoiResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    row[3] != null ? new BigDecimal(row[3].toString()) : null,
                    row[4] != null ? new BigDecimal(row[4].toString()) : null,
                    ((Number) row[5]).longValue()
            ));
        }

        redisUtils.setObject(cacheKey, result, CACHE_HOURS, TimeUnit.HOURS);
        return result;
    }

    @Override
    public void evictUserLeaderboards() {
        for (String type : LB_TYPES) {
            for (String period : LB_PERIODS) {
                redisUtils.delete(String.format("leaderboard:%s:%s", type, period));
            }
        }
    }

    private LocalDateTime weekStart() {
        return LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
