package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AdminAnnouncementDetailResponse;
import com.smartcampus.dto.response.AdminAnnouncementListItemResponse;
import com.smartcampus.service.AdminAnnouncementService;
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

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    @GetMapping
    public Result<PageResponse<AdminAnnouncementListItemResponse>> getAnnouncements(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminAnnouncementService.getAnnouncements(keyword, status, pinned, page, size));
    }

    @GetMapping("/{announcementId}")
    public Result<AdminAnnouncementDetailResponse> getAnnouncementDetail(@PathVariable Long announcementId) {
        return Result.success(adminAnnouncementService.getAnnouncementDetail(announcementId));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public Result<AdminAnnouncementDetailResponse> createAnnouncement(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Short status,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminAnnouncementService.createAnnouncement(title, summary, content, pinned, status, getCurrentUserId(authentication), coverImage));
    }

    @PutMapping(value = "/{announcementId}", consumes = {"multipart/form-data"})
    public Result<AdminAnnouncementDetailResponse> updateAnnouncement(
            @PathVariable Long announcementId,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String content,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Boolean removeCoverImage,
            Authentication authentication,
            @RequestParam(required = false) MultipartFile coverImage
    ) {
        return Result.success(adminAnnouncementService.updateAnnouncement(announcementId, title, summary, content, pinned, status, removeCoverImage, getCurrentUserId(authentication), coverImage));
    }

    @PutMapping("/{announcementId}/publish")
    public Result<AdminAnnouncementDetailResponse> updatePublishStatus(
            @PathVariable Long announcementId,
            @RequestParam Boolean published,
            Authentication authentication
    ) {
        return Result.success(adminAnnouncementService.updatePublishStatus(announcementId, published, getCurrentUserId(authentication)));
    }

    @PutMapping("/{announcementId}/pin")
    public Result<AdminAnnouncementDetailResponse> updatePinnedStatus(
            @PathVariable Long announcementId,
            @RequestParam Boolean pinned,
            Authentication authentication
    ) {
        return Result.success(adminAnnouncementService.updatePinnedStatus(announcementId, pinned, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{announcementId}")
    public Result<Void> deleteAnnouncement(@PathVariable Long announcementId, Authentication authentication) {
        adminAnnouncementService.deleteAnnouncement(announcementId, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
