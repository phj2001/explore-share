package com.smartcampus.service.impl;

import com.smartcampus.dto.response.HotPoiResponse;
import com.smartcampus.dto.response.LeaderboardItemResponse;
import com.smartcampus.entity.User;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
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

    private LocalDateTime weekStart() {
        return LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
