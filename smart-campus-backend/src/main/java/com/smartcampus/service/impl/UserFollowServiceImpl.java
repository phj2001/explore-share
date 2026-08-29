package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.FollowStatusResponse;
import com.smartcampus.dto.response.FollowUserItemResponse;
import com.smartcampus.entity.User;
import com.smartcampus.entity.UserFollow;
import com.smartcampus.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.smartcampus.repository.UserFollowRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.ProfileVisibilityGuard;
import com.smartcampus.service.NotificationService;
import com.smartcampus.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProfileVisibilityGuard profileVisibilityGuard;

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        User follower = getRequiredUser(followerId);
        User following = getRequiredUser(followingId);

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            try {
                UserFollow uf = new UserFollow();
                uf.setFollower(follower);
                uf.setFollowing(following);
                userFollowRepository.save(uf);

                String actorName = follower.getDisplayName() != null ? follower.getDisplayName() : follower.getUsername();
                notificationService.sendNotification(followingId, followerId, "FOLLOW",
                        actorName + " 关注了你", null, "USER", followerId);
            } catch (DataIntegrityViolationException ignored) {
                // 并发场景下唯一约束冲突，幂等处理
            }
        }
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(Long currentUserId, Long targetUserId) {
        User target = getRequiredUser(targetUserId);

        boolean following = currentUserId != null
                && userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean follower = currentUserId != null
                && userFollowRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId);

        // 受限查看者：两计数置 0（布尔保留，前端引导关注需要）；关注成功后无缓存残留、立即可见
        if (!profileVisibilityGuard.isContentVisible(target, currentUserId)) {
            return new FollowStatusResponse(following, follower, 0L, 0L);
        }

        long followingCount = userFollowRepository.countByFollowerId(targetUserId);
        long followerCount = userFollowRepository.countByFollowingId(targetUserId);

        return new FollowStatusResponse(following, follower, followingCount, followerCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserItemResponse> getFollowingList(Long userId, Integer page, Integer size, Long viewerId) {
        User targetUser = getRequiredUser(userId);
        profileVisibilityGuard.checkContentVisible(targetUser, viewerId);
        Pageable pageable = buildPageable(page, size);

        Page<UserFollow> result = userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable);

        List<FollowUserItemResponse> records = result.getContent().stream()
                .map(this::toFollowingResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUserItemResponse> getFollowerList(Long userId, Integer page, Integer size, Long viewerId) {
        User targetUser = getRequiredUser(userId);
        profileVisibilityGuard.checkContentVisible(targetUser, viewerId);
        Pageable pageable = buildPageable(page, size);

        Page<UserFollow> result = userFollowRepository.findByFollowingIdOrderByCreatedAtDesc(userId, pageable);

        List<FollowUserItemResponse> records = result.getContent().stream()
                .map(this::toFollowerResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long userId) {
        return userFollowRepository.findFollowingIdByFollowerId(userId);
    }

    private FollowUserItemResponse toFollowingResponse(UserFollow uf) {
        User user = uf.getFollowing();
        return new FollowUserItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                uf.getCreatedAt()
        );
    }

    private FollowUserItemResponse toFollowerResponse(UserFollow uf) {
        User user = uf.getFollower();
        return new FollowUserItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                uf.getCreatedAt()
        );
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
