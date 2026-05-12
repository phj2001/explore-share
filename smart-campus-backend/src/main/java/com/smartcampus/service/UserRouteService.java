package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreateUserRouteRequest;
import com.smartcampus.dto.response.UserRouteListItemResponse;
import com.smartcampus.dto.response.UserRouteResponse;

public interface UserRouteService {

    UserRouteResponse createRoute(Long userId, CreateUserRouteRequest request);

    UserRouteResponse updateRoute(Long routeId, Long userId, CreateUserRouteRequest request);

    void deleteRoute(Long routeId, Long userId);

    UserRouteResponse getRouteDetail(Long routeId, Long currentUserId);

    PageResponse<UserRouteListItemResponse> getPublicRoutes(Integer page, Integer size);

    PageResponse<UserRouteListItemResponse> getMyRoutes(Long userId, Integer page, Integer size);

    PageResponse<UserRouteListItemResponse> getMyFavoriteRoutes(Long userId, Integer page, Integer size);

    boolean toggleLike(Long routeId, Long userId);

    boolean toggleFavorite(Long routeId, Long userId);

    PageResponse<UserRouteListItemResponse> adminGetRoutes(Short status, String keyword, Integer page, Integer size);

    void adminReviewRoute(Long routeId, Short newStatus, String rejectReason, Long adminUserId);
}
