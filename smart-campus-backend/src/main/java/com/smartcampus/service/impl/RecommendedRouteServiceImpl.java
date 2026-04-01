package com.smartcampus.service.impl;

import com.smartcampus.dto.response.RecommendedRouteDetailResponse;
import com.smartcampus.dto.response.RecommendedRouteListItemResponse;
import com.smartcampus.entity.RecommendedRoute;
import com.smartcampus.enums.RouteMode;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.RecommendedRouteRepository;
import com.smartcampus.service.RecommendedRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendedRouteServiceImpl implements RecommendedRouteService {

    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 12;

    private final RecommendedRouteRepository recommendedRouteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedRouteListItemResponse> getPublishedRoutes(Integer limit) {
        int normalizedLimit = DEFAULT_LIMIT;
        if (limit != null) {
            normalizedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        }

        return recommendedRouteRepository.findByStatusOrderBySortOrderAscPublishedAtDescIdDesc(RecommendedRoute.STATUS_PUBLISHED)
                .stream()
                .limit(normalizedLimit)
                .map(route -> RecommendedRouteListItemResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendedRouteDetailResponse getPublishedRouteDetail(Long routeId) {
        RecommendedRoute route = recommendedRouteRepository.findByIdAndStatus(routeId, RecommendedRoute.STATUS_PUBLISHED)
                .orElseThrow(() -> new BusinessException(404, "推荐路线不存在"));
        return RecommendedRouteDetailResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode()));
    }

    private String resolveModeLabel(String mode) {
        try {
            return RouteMode.fromValue(mode).getLabel();
        } catch (IllegalArgumentException ex) {
            return mode;
        }
    }
}
