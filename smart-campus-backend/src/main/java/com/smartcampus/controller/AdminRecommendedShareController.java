package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.AdminCreateRecommendedShareRequest;
import com.smartcampus.dto.request.AdminUpdateRecommendedShareRequest;
import com.smartcampus.dto.response.AdminRecommendedShareCandidateResponse;
import com.smartcampus.dto.response.AdminRecommendedShareListItemResponse;
import com.smartcampus.service.RecommendedShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/recommended-shares")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminRecommendedShareController {

    private final RecommendedShareService recommendedShareService;

    @GetMapping
    public Result<PageResponse<AdminRecommendedShareListItemResponse>> getRecommendedShares(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(recommendedShareService.getRecommendedShares(keyword, page, size));
    }

    @GetMapping("/candidates")
    public Result<PageResponse<AdminRecommendedShareCandidateResponse>> getCandidateShares(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poiId,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(recommendedShareService.getCandidateShares(keyword, poiId, recommended, page, size));
    }

    @PostMapping
    public Result<AdminRecommendedShareListItemResponse> createRecommendedShare(
            @Valid @RequestBody AdminCreateRecommendedShareRequest request,
            Authentication authentication
    ) {
        return Result.success(recommendedShareService.createRecommendedShare(request, getCurrentUserId(authentication)));
    }

    @PutMapping("/{id}")
    public Result<AdminRecommendedShareListItemResponse> updateRecommendedShare(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRecommendedShareRequest request,
            Authentication authentication
    ) {
        return Result.success(recommendedShareService.updateRecommendedShare(id, request, getCurrentUserId(authentication)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRecommendedShare(@PathVariable Long id, Authentication authentication) {
        recommendedShareService.deleteRecommendedShare(id, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
