package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AdminRecommendedRouteDetailResponse;
import com.smartcampus.dto.response.AdminRecommendedRouteListItemResponse;
import com.smartcampus.service.AdminRecommendedRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommended-routes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRecommendedRouteController {

    private final AdminRecommendedRouteService adminRecommendedRouteService;

    @GetMapping
    public Result<PageResponse<AdminRecommendedRouteListItemResponse>> getRoutes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) String defaultMode,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminRecommendedRouteService.getRoutes(keyword, status, defaultMode, page, size));
    }

    @GetMapping("/{routeId}")
    public Result<AdminRecommendedRouteDetailResponse> getRouteDetail(@PathVariable Long routeId) {
        return Result.success(adminRecommendedRouteService.getRouteDetail(routeId));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public Result<AdminRecommendedRouteDetailResponse> createRoute(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String description,
            @RequestParam(required = false) String recommendationText,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) String defaultMode,
            @RequestParam(required = false) Short status,
            @RequestParam List<Long> poiIds,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminRecommendedRouteService.createRoute(
                title,
                summary,
                description,
                recommendationText,
                sortOrder,
                defaultMode,
                status,
                poiIds,
                getCurrentUserId(authentication),
                coverImage
        ));
    }

    @PutMapping(value = "/{routeId}", consumes = {"multipart/form-data"})
    public Result<AdminRecommendedRouteDetailResponse> updateRoute(
            @PathVariable Long routeId,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String description,
            @RequestParam(required = false) String recommendationText,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) String defaultMode,
            @RequestParam(required = false) Short status,
            @RequestParam List<Long> poiIds,
            @RequestParam(required = false) Boolean removeCoverImage,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminRecommendedRouteService.updateRoute(
                routeId,
                title,
                summary,
                description,
                recommendationText,
                sortOrder,
                defaultMode,
                status,
                poiIds,
                removeCoverImage,
                getCurrentUserId(authentication),
                coverImage
        ));
    }

    @PutMapping("/{routeId}/publish")
    public Result<AdminRecommendedRouteDetailResponse> updatePublishStatus(
            @PathVariable Long routeId,
            @RequestParam Boolean published,
            Authentication authentication
    ) {
        return Result.success(adminRecommendedRouteService.updatePublishStatus(routeId, published, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{routeId}")
    public Result<Void> deleteRoute(@PathVariable Long routeId, Authentication authentication) {
        adminRecommendedRouteService.deleteRoute(routeId, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
