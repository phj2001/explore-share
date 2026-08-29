package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.dto.response.POIShareResponse;
import com.smartcampus.dto.response.UserCheckInItemResponse;
import com.smartcampus.dto.response.UserPublicProfileResponse;
import com.smartcampus.entity.POICheckIn;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareLike;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIReviewRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.ProfileVisibilityGuard;
import com.smartcampus.service.UserPublicProfileService;
import com.smartcampus.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserPublicProfileServiceImpl implements UserPublicProfileService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_REPLY_SIZE = 3;
    /** 公开统计缓存 TTL：与站点既有计数延迟水平一致（点赞 120s、排行榜 1h），不做写时失效 */
    private static final long STATS_CACHE_TTL_SECONDS = 300L;
    private static final String STATS_CACHE_PREFIX = "public:stats:";

    private final UserRepository userRepository;
    private final ProfileVisibilityGuard profileVisibilityGuard;
    private final POICheckInRepository poiCheckInRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIReviewRepository poiReviewRepository;
    private final RedisUtils redisUtils;

    /** 仅缓存 4 个计数（昵称/头像等资料本体保持实时），作为 RedisUtils.setObject 的 JSON 载荷 */
    record UserPublicStats(long checkInCount, long shareCount, long receivedLikeCount, long reviewCount) {
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(Long userId, Long viewerId) {
        User user = getRequiredUser(userId);

        // 受限查看者：在取 stats 缓存之前短路，返回仅含身份骨架的精简响应
        //（username 不暴露、bio/时间为空、4 计数为 0），前端据 contentVisible=false 渲染占位卡
        if (!profileVisibilityGuard.isContentVisible(user, viewerId)) {
            return new UserPublicProfileResponse(
                    user.getId(),
                    null,
                    user.getDisplayName(),
                    normalizeAvatarUrl(user.getAvatarUrl()),
                    null,
                    null,
                    0L,
                    0L,
                    0L,
                    0L,
                    user.getStatus(),
                    user.getProfileVisibility(),
                    false
            );
        }

        UserPublicStats stats = getCachedStats(userId);

        return new UserPublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                normalizeAvatarUrl(user.getAvatarUrl()),
                user.getBio(),
                user.getCreatedAt(),
                stats.checkInCount(),
                stats.shareCount(),
                stats.receivedLikeCount(),
                stats.reviewCount(),
                user.getStatus(),
                user.getProfileVisibility(),
                true
        );
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.startsWith("http") && !avatarUrl.startsWith("/")) {
            return "/" + avatarUrl;
        }
        return avatarUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIShareResponse> getUserPublicShares(Long userId, Integer page, Integer size, Long viewerId) {
        User targetUser = getRequiredUser(userId);
        profileVisibilityGuard.checkContentVisible(targetUser, viewerId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIShare> sharePage = poiShareRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<POIShare> shares = sharePage.getContent();

        List<Long> shareIds = shares.stream().map(POIShare::getId).toList();

        // 浏览者信息（游客 viewerId=null）：用于回填 likedByCurrentUser 与 canDelete（本人/超管）
        User viewer = viewerId == null ? null : userRepository.findById(viewerId).orElse(null);
        Set<Long> likedShareIds = (viewer == null || shareIds.isEmpty())
                ? Set.of()
                : Set.copyOf(poiShareLikeRepository.findLikedShareIdsByUserId(shareIds, viewer.getId()));

        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        List<POIShareResponse> records = shares.stream()
                .map(share -> POIShareResponse.fromEntity(
                        share,
                        viewer == null ? null : viewer.getId(),
                        viewer == null ? null : viewer.getRole(),
                        likeCountMap.getOrDefault(share.getId(), 0L),
                        likedShareIds.contains(share.getId()),
                        replyCountMap.getOrDefault(share.getId(), 0L),
                        List.of()
                ))
                .toList();

        return new PageResponse<>(records, sharePage.getNumber(), sharePage.getSize(), sharePage.getTotalElements(), sharePage.hasNext());
    }

    /** 读缓存，miss 则实时统计 4 个 count 并写回（注意 receivedLike 用 countReceivedLikesByUserId，即“收到的赞”） */
    private UserPublicStats getCachedStats(Long userId) {
        UserPublicStats cached = redisUtils.getObject(STATS_CACHE_PREFIX + userId, UserPublicStats.class);
        if (cached != null) {
            return cached;
        }

        UserPublicStats stats = new UserPublicStats(
                poiCheckInRepository.countByUserId(userId),
                poiShareRepository.countByUserId(userId),
                poiShareLikeRepository.countReceivedLikesByUserId(userId),
                poiReviewRepository.countByUserId(userId)
        );
        redisUtils.setObject(STATS_CACHE_PREFIX + userId, stats, STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserCheckInItemResponse> getUserCheckIns(Long userId, Integer page, Integer size, Long viewerId) {
        User targetUser = getRequiredUser(userId);
        profileVisibilityGuard.checkContentVisible(targetUser, viewerId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POICheckIn> result = poiCheckInRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UserCheckInItemResponse> records = result.getContent().stream()
                .map(this::toCheckInItemResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private UserCheckInItemResponse toCheckInItemResponse(POICheckIn checkIn) {
        return new UserCheckInItemResponse(
                checkIn.getId(),
                checkIn.getPoi().getId(),
                checkIn.getPoi().getName(),
                checkIn.getPoi().getCategory(),
                checkIn.getPoi().getLatitude(),
                checkIn.getPoi().getLongitude(),
                checkIn.getCreatedAt()
        );
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }
}
