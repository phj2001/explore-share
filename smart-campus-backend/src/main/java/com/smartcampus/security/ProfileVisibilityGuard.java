package com.smartcampus.security;

import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 个人主页可见性守卫：统一判定 viewer 对 target 主页内容的可见性。
 * profile 端点用 {@link #isContentVisible} 走精简响应；内容端点（打卡/分享/关注列表/成就）
 * 用 {@link #checkContentVisible} 抛 403，形成防御纵深，防止绕过主页直取内容。
 */
@Component
@RequiredArgsConstructor
public class ProfileVisibilityGuard {

    private final UserFollowRepository userFollowRepository;

    public boolean isContentVisible(User target, Long viewerId) {
        if (viewerId != null && viewerId.equals(target.getId())) {
            return true;
        }
        return switch (ProfileVisibility.fromCode(target.getProfileVisibility())) {
            case PUBLIC -> true;
            case FOLLOWERS -> viewerId != null
                    && userFollowRepository.existsByFollowerIdAndFollowingId(viewerId, target.getId());
            case PRIVATE -> false;
        };
    }

    public void checkContentVisible(User target, Long viewerId) {
        if (!isContentVisible(target, viewerId)) {
            throw new BusinessException(403, "该用户的内容仅对其本人或关注者可见");
        }
    }
}
