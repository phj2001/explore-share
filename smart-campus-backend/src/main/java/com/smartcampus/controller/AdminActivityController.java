package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AdminActivityDetailResponse;
import com.smartcampus.dto.response.AdminActivityListItemResponse;
import com.smartcampus.service.AdminActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    @GetMapping
    public Result<PageResponse<AdminActivityListItemResponse>> getActivities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Long poiId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminActivityService.getActivities(keyword, status, poiId, startTime, endTime, page, size));
    }

    @GetMapping("/{activityId}")
    public Result<AdminActivityDetailResponse> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(adminActivityService.getActivityDetail(activityId));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public Result<AdminActivityDetailResponse> createActivity(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam(required = false) Long poiId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Short status,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminActivityService.createActivity(
                title,
                summary,
                content,
                poiId,
                startTime,
                endTime,
                status,
                getCurrentUserId(authentication),
                coverImage
        ));
    }

    @PutMapping(value = "/{activityId}", consumes = {"multipart/form-data"})
    public Result<AdminActivityDetailResponse> updateActivity(
            @PathVariable Long activityId,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam(required = false) Long poiId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Boolean removeCoverImage,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminActivityService.updateActivity(
                activityId,
                title,
                summary,
                content,
                poiId,
                startTime,
                endTime,
                status,
                removeCoverImage,
                getCurrentUserId(authentication),
                coverImage
        ));
    }

    @PutMapping("/{activityId}/publish")
    public Result<AdminActivityDetailResponse> updatePublishStatus(
            @PathVariable Long activityId,
            @RequestParam Boolean published,
            Authentication authentication
    ) {
        return Result.success(adminActivityService.updatePublishStatus(activityId, published, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{activityId}")
    public Result<Void> deleteActivity(@PathVariable Long activityId, Authentication authentication) {
        adminActivityService.deleteActivity(activityId, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
