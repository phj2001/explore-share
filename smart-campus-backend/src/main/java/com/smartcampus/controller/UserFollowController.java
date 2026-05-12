package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.FollowStatusResponse;
import com.smartcampus.dto.response.FollowUserItemResponse;
import com.smartcampus.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    @PostMapping("/api/users/{userId}/follow")
    public Result<Void> follow(@PathVariable Long userId, Authentication authentication) {
        userFollowService.follow(getRequiredUserId(authentication), userId);
        return Result.success(null);
    }

    @DeleteMapping("/api/users/{userId}/follow")
    public Result<Void> unfollow(@PathVariable Long userId, Authentication authentication) {
        userFollowService.unfollow(getRequiredUserId(authentication), userId);
        return Result.success(null);
    }

    @GetMapping("/api/users/{userId}/follow-status")
    public Result<FollowStatusResponse> getFollowStatus(@PathVariable Long userId, Authentication authentication) {
        return Result.success(userFollowService.getFollowStatus(getOptionalUserId(authentication), userId));
    }

    @GetMapping("/api/users/{userId}/following")
    public Result<PageResponse<FollowUserItemResponse>> getFollowingList(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userFollowService.getFollowingList(userId, page, size));
    }

    @GetMapping("/api/users/{userId}/followers")
    public Result<PageResponse<FollowUserItemResponse>> getFollowerList(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userFollowService.getFollowerList(userId, page, size));
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
