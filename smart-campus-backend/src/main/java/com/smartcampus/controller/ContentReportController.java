package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.CreateContentReportRequest;
import com.smartcampus.service.ContentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-reports")
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService contentReportService;

    @PostMapping("/shares/{shareId}")
    public Result<Void> createShareReport(
            @PathVariable Long shareId,
            @Valid @RequestBody CreateContentReportRequest request,
            Authentication authentication
    ) {
        contentReportService.createShareReport(shareId, getRequiredUserId(authentication), request.getReasonCode(), request.getReasonDetail());
        return Result.success();
    }

    @PostMapping("/replies/{replyId}")
    public Result<Void> createReplyReport(
            @PathVariable Long replyId,
            @Valid @RequestBody CreateContentReportRequest request,
            Authentication authentication
    ) {
        contentReportService.createReplyReport(replyId, getRequiredUserId(authentication), request.getReasonCode(), request.getReasonDetail());
        return Result.success();
    }

    private Long getRequiredUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
