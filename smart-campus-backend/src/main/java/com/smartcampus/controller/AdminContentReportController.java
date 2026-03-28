package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.AdminReviewContentReportRequest;
import com.smartcampus.dto.response.AdminContentReportDetailResponse;
import com.smartcampus.dto.response.AdminContentReportListItemResponse;
import com.smartcampus.service.AdminContentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminContentReportController {

    private final AdminContentReportService adminContentReportService;

    @GetMapping
    public Result<PageResponse<AdminContentReportListItemResponse>> getReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short targetType,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Short reasonCode,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminContentReportService.getReports(keyword, targetType, status, reasonCode, page, size));
    }

    @GetMapping("/{reportId}")
    public Result<AdminContentReportDetailResponse> getReportDetail(@PathVariable Long reportId) {
        return Result.success(adminContentReportService.getReportDetail(reportId));
    }

    @PutMapping("/{reportId}/review")
    public Result<AdminContentReportDetailResponse> reviewReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReviewContentReportRequest request,
            Authentication authentication
    ) {
        return Result.success(adminContentReportService.reviewReport(
                reportId,
                request.getStatus(),
                request.getAction(),
                request.getReviewNote(),
                getCurrentUserId(authentication)
        ));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
