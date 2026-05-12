package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.CreatePOIReviewRequest;
import com.smartcampus.dto.response.POIRatingSummaryResponse;
import com.smartcampus.dto.response.POIReviewResponse;
import com.smartcampus.service.POIReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class POIReviewController {

    private final POIReviewService poiReviewService;

    @GetMapping("/api/pois/{poiId}/rating")
    public Result<POIRatingSummaryResponse> getRatingSummary(@PathVariable Long poiId) {
        return Result.success(poiReviewService.getRatingSummary(poiId));
    }

    @GetMapping("/api/pois/{poiId}/reviews")
    public Result<PageResponse<POIReviewResponse>> getPoiReviews(
            @PathVariable Long poiId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        return Result.success(poiReviewService.getPoiReviews(poiId, page, size, getOptionalUserId(authentication)));
    }

    @PostMapping("/api/pois/{poiId}/reviews")
    public Result<POIReviewResponse> createOrUpdateReview(
            @PathVariable Long poiId,
            @Valid @RequestBody CreatePOIReviewRequest request,
            Authentication authentication) {
        return Result.success(poiReviewService.createOrUpdateReview(poiId, getRequiredUserId(authentication), request));
    }

    @DeleteMapping("/api/pois/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long reviewId, Authentication authentication) {
        poiReviewService.deleteReview(reviewId, getRequiredUserId(authentication));
        return Result.success();
    }

    @GetMapping("/api/users/me/reviews")
    public Result<PageResponse<POIReviewResponse>> getUserReviews(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(poiReviewService.getUserReviews(getRequiredUserId(authentication), page, size));
    }

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<PageResponse<POIReviewResponse>> getAdminReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long poiId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(poiReviewService.getAdminReviews(keyword, poiId, minRating, maxRating, page, size));
    }

    @DeleteMapping("/api/admin/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Result<Void> adminDeleteReview(@PathVariable Long reviewId) {
        poiReviewService.adminDeleteReview(reviewId);
        return Result.success();
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
