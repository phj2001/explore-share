package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AdminReplyListItemResponse;
import com.smartcampus.dto.response.AdminShareDetailResponse;
import com.smartcampus.dto.response.AdminShareListItemResponse;
import com.smartcampus.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminContentController {

    private final AdminContentService adminContentService;

    @GetMapping("/shares")
    public Result<PageResponse<AdminShareListItemResponse>> getShares(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poiId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminContentService.getShares(keyword, poiId, startTime, endTime, page, size));
    }

    @GetMapping("/shares/{shareId}")
    public Result<AdminShareDetailResponse> getShareDetail(@PathVariable Long shareId) {
        return Result.success(adminContentService.getShareDetail(shareId));
    }

    @DeleteMapping("/shares/{shareId}")
    public Result<Void> deleteShare(@PathVariable Long shareId, Authentication authentication) {
        adminContentService.deleteShare(shareId, getCurrentUserId(authentication));
        return Result.success();
    }

    @GetMapping("/replies")
    public Result<PageResponse<AdminReplyListItemResponse>> getReplies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shareId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminContentService.getReplies(keyword, shareId, startTime, endTime, page, size));
    }

    @DeleteMapping("/replies/{replyId}")
    public Result<Void> deleteReply(@PathVariable Long replyId, Authentication authentication) {
        adminContentService.deleteReply(replyId, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
