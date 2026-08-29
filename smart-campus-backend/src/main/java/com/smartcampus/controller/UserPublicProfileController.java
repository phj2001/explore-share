package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.POIShareResponse;
import com.smartcampus.dto.response.UserCheckInItemResponse;
import com.smartcampus.dto.response.UserPublicProfileResponse;
import com.smartcampus.service.UserPublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserPublicProfileController {

    private final UserPublicProfileService userPublicProfileService;

    /** 公开接口（游客可访问）：受限查看者拿到精简响应（contentVisible=false），由前端渲染占位 */
    @GetMapping("/api/users/{userId}/profile")
    public Result<UserPublicProfileResponse> getPublicProfile(@PathVariable Long userId, Authentication authentication) {
        return Result.success(userPublicProfileService.getPublicProfile(userId, getOptionalUserId(authentication)));
    }

    /** 公开接口（游客可访问）：浏览者已登录时回填 likedByCurrentUser / canDelete，游客则为 false */
    @GetMapping("/api/users/{userId}/shares")
    public Result<PageResponse<POIShareResponse>> getUserPublicShares(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return Result.success(userPublicProfileService.getUserPublicShares(userId, page, size, getOptionalUserId(authentication)));
    }

    /** 公开接口（游客可访问）：受限查看者 403，防绕过主页直取内容 */
    @GetMapping("/api/users/{userId}/checkins")
    public Result<PageResponse<UserCheckInItemResponse>> getUserCheckIns(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return Result.success(userPublicProfileService.getUserCheckIns(userId, page, size, getOptionalUserId(authentication)));
    }

    private Long getOptionalUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
