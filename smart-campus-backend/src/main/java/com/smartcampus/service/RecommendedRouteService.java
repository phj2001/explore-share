package com.smartcampus.service;

import com.smartcampus.dto.response.RecommendedRouteDetailResponse;
import com.smartcampus.dto.response.RecommendedRouteListItemResponse;

import java.util.List;

public interface RecommendedRouteService {

    List<RecommendedRouteListItemResponse> getPublishedRoutes(Integer limit);

    RecommendedRouteDetailResponse getPublishedRouteDetail(Long routeId);
}
