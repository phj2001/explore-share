package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.ReviewPOIApplicationRequest;
import com.smartcampus.dto.response.AdminPOIApplicationListItemResponse;
import com.smartcampus.service.POIApplicationService;
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
@RequestMapping("/api/admin/poi-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminPOIApplicationController {

    private final POIApplicationService poiApplicationService;

    @GetMapping
    public Result<PageResponse<AdminPOIApplicationListItemResponse>> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(poiApplicationService.getAdminList(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<AdminPOIApplicationListItemResponse> getDetail(@PathVariable Long id) {
        return Result.success(poiApplicationService.getAdminDetail(id));
    }

    @PutMapping("/{id}/review")
    public Result<Void> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody ReviewPOIApplicationRequest request,
            Authentication authentication) {
        Long reviewerId = getRequiredUserId(authentication);
        poiApplicationService.reviewApplication(id, reviewerId, request);
        return Result.success(null);
    }

    private Long getRequiredUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
