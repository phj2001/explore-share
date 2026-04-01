package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminRecommendedRouteDetailResponse;
import com.smartcampus.dto.response.AdminRecommendedRouteListItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminRecommendedRouteService {

    PageResponse<AdminRecommendedRouteListItemResponse> getRoutes(
            String keyword,
            Short status,
            String defaultMode,
            Integer page,
            Integer size
    );

    AdminRecommendedRouteDetailResponse getRouteDetail(Long routeId);

    AdminRecommendedRouteDetailResponse createRoute(
            String title,
            String summary,
            String description,
            String recommendationText,
            Integer sortOrder,
            String defaultMode,
            Short status,
            List<Long> poiIds,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminRecommendedRouteDetailResponse updateRoute(
            Long routeId,
            String title,
            String summary,
            String description,
            String recommendationText,
            Integer sortOrder,
            String defaultMode,
            Short status,
            List<Long> poiIds,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminRecommendedRouteDetailResponse updatePublishStatus(Long routeId, Boolean published, Long operatorUserId);

    void deleteRoute(Long routeId, Long operatorUserId);
}
