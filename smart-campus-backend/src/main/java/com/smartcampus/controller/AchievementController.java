package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AchievementResponse;
import com.smartcampus.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/api/achievements")
    public Result<List<AchievementResponse>> getAllDefinitions() {
        return Result.success(achievementService.getAllDefinitions());
    }

    @GetMapping("/api/users/me/achievements")
    public Result<List<AchievementResponse>> getMyAchievements(Authentication authentication) {
        Long userId = getRequiredUserId(authentication);
        // 本人视角恒可见（viewer == target）
        return Result.success(achievementService.getUserAchievements(userId, userId));
    }

    /** 公开接口（游客可访问）：受限查看者 403 */
    @GetMapping("/api/users/{userId}/achievements")
    public Result<List<AchievementResponse>> getUserAchievements(@PathVariable Long userId, Authentication authentication) {
        return Result.success(achievementService.getUserAchievements(userId, getOptionalUserId(authentication)));
    }

    private Long getOptionalUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private Long getRequiredUserId(Authentication authentication) {
        Long userId = getOptionalUserId(authentication);
        if (userId == null) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        return userId;
    }
}
