package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreatePOIReviewRequest;
import com.smartcampus.dto.response.POIRatingSummaryResponse;
import com.smartcampus.dto.response.POIReviewResponse;

public interface POIReviewService {

    POIRatingSummaryResponse getRatingSummary(Long poiId);

    PageResponse<POIReviewResponse> getPoiReviews(Long poiId, Integer page, Integer size, Long currentUserId);

    POIReviewResponse createOrUpdateReview(Long poiId, Long userId, CreatePOIReviewRequest request);

    void deleteReview(Long reviewId, Long userId);

    PageResponse<POIReviewResponse> getUserReviews(Long userId, Integer page, Integer size);

    PageResponse<POIReviewResponse> getAdminReviews(String keyword, Long poiId, Integer minRating, Integer maxRating, Integer page, Integer size);

    void adminDeleteReview(Long reviewId);
}
