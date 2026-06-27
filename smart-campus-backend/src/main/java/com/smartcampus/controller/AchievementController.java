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
        return Result.success(achievementService.getUserAchievements(userId));
    }

    @GetMapping("/api/users/{userId}/achievements")
    public Result<List<AchievementResponse>> getUserAchievements(@PathVariable Long userId) {
        return Result.success(achievementService.getUserAchievements(userId));
    }

    private Long getRequiredUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
