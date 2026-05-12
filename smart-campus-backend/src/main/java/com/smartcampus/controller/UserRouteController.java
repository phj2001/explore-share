package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.CreateUserRouteRequest;
import com.smartcampus.dto.response.UserRouteListItemResponse;
import com.smartcampus.dto.response.UserRouteResponse;
import com.smartcampus.service.UserRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserRouteController {

    private final UserRouteService userRouteService;

    @PostMapping("/api/user-routes")
    public Result<UserRouteResponse> createRoute(
            @Valid @RequestBody CreateUserRouteRequest request,
            Authentication authentication) {
        return Result.success(userRouteService.createRoute(getRequiredUserId(authentication), request));
    }

    @PutMapping("/api/user-routes/{id}")
    public Result<UserRouteResponse> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRouteRequest request,
            Authentication authentication) {
        return Result.success(userRouteService.updateRoute(id, getRequiredUserId(authentication), request));
    }

    @DeleteMapping("/api/user-routes/{id}")
    public Result<Void> deleteRoute(@PathVariable Long id, Authentication authentication) {
        userRouteService.deleteRoute(id, getRequiredUserId(authentication));
        return Result.success(null);
    }

    @GetMapping("/api/user-routes")
    public Result<PageResponse<UserRouteListItemResponse>> getPublicRoutes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userRouteService.getPublicRoutes(page, size));
    }

    @GetMapping("/api/user-routes/{id}")
    public Result<UserRouteResponse> getRouteDetail(@PathVariable Long id, Authentication authentication) {
        return Result.success(userRouteService.getRouteDetail(id, getOptionalUserId(authentication)));
    }

    @PostMapping("/api/user-routes/{id}/like")
    public Result<Map<String, Boolean>> toggleLike(@PathVariable Long id, Authentication authentication) {
        boolean liked = userRouteService.toggleLike(id, getRequiredUserId(authentication));
        return Result.success(Map.of("liked", liked));
    }

    @PostMapping("/api/user-routes/{id}/favorite")
    public Result<Map<String, Boolean>> toggleFavorite(@PathVariable Long id, Authentication authentication) {
        boolean favorited = userRouteService.toggleFavorite(id, getRequiredUserId(authentication));
        return Result.success(Map.of("favorited", favorited));
    }

    @GetMapping("/api/users/me/routes")
    public Result<PageResponse<UserRouteListItemResponse>> getMyRoutes(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userRouteService.getMyRoutes(getRequiredUserId(authentication), page, size));
    }

    @GetMapping("/api/users/me/favorite-routes")
    public Result<PageResponse<UserRouteListItemResponse>> getMyFavoriteRoutes(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userRouteService.getMyFavoriteRoutes(getRequiredUserId(authentication), page, size));
    }

    @GetMapping("/api/admin/user-routes")
    public Result<PageResponse<UserRouteListItemResponse>> adminGetRoutes(
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(userRouteService.adminGetRoutes(status, keyword, page, size));
    }

    @PutMapping("/api/admin/user-routes/{id}/status")
    public Result<Void> adminReviewRoute(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Object statusObj = body.get("status");
        if (statusObj == null) {
            throw new IllegalArgumentException("缺少 status 字段");
        }
        Short newStatus = ((Number) statusObj).shortValue();
        String rejectReason = (String) body.get("rejectReason");
        Long adminUserId = getOptionalUserId(authentication);
        userRouteService.adminReviewRoute(id, newStatus, rejectReason, adminUserId);
        return Result.success(null);
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
