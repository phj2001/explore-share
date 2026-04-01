package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.AdminCreateRecommendedShareRequest;
import com.smartcampus.dto.request.AdminUpdateRecommendedShareRequest;
import com.smartcampus.dto.response.AdminRecommendedShareCandidateResponse;
import com.smartcampus.dto.response.AdminRecommendedShareListItemResponse;
import com.smartcampus.dto.response.RecommendedSharePublicResponse;

import java.util.List;

public interface RecommendedShareService {

    PageResponse<AdminRecommendedShareListItemResponse> getRecommendedShares(String keyword, Integer page, Integer size);

    PageResponse<AdminRecommendedShareCandidateResponse> getCandidateShares(
            String keyword,
            Long poiId,
            Boolean recommended,
            Integer page,
            Integer size
    );

    AdminRecommendedShareListItemResponse createRecommendedShare(AdminCreateRecommendedShareRequest request, Long operatorUserId);

    AdminRecommendedShareListItemResponse updateRecommendedShare(Long id, AdminUpdateRecommendedShareRequest request, Long operatorUserId);

    void deleteRecommendedShare(Long id, Long operatorUserId);

    List<RecommendedSharePublicResponse> getPublicRecommendedShares(Integer limit);
}
