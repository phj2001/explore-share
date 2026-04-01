package com.smartcampus.dto.response;

import com.smartcampus.entity.RecommendedRoute;
import com.smartcampus.entity.RecommendedRouteWaypoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedRouteDetailResponse {

    private Long id;

    private String title;

    private String summary;

    private String description;

    private String recommendationText;

    private String coverImageUrl;

    private Integer sortOrder;

    private String defaultMode;

    private String defaultModeLabel;

    private List<RouteWaypointResponse> waypoints;

    public static RecommendedRouteDetailResponse fromEntity(RecommendedRoute entity, String defaultModeLabel) {
        List<RouteWaypointResponse> waypoints = entity.getWaypoints().stream()
                .sorted(Comparator.comparing(RecommendedRouteWaypoint::getSortOrder).thenComparing(RecommendedRouteWaypoint::getId))
                .map(RouteWaypointResponse::fromEntity)
                .toList();
        return new RecommendedRouteDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getRecommendationText(),
                entity.getCoverImageUrl(),
                entity.getSortOrder(),
                entity.getDefaultMode(),
                defaultModeLabel,
                waypoints
        );
    }
}
