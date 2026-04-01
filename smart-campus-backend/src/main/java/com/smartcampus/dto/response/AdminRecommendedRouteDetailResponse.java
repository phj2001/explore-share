package com.smartcampus.dto.response;

import com.smartcampus.entity.RecommendedRoute;
import com.smartcampus.entity.RecommendedRouteWaypoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecommendedRouteDetailResponse {

    private Long id;

    private String title;

    private String summary;

    private String description;

    private String recommendationText;

    private String coverImageUrl;

    private Integer sortOrder;

    private String defaultMode;

    private String defaultModeLabel;

    private Short status;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<RouteWaypointResponse> waypoints;

    public static AdminRecommendedRouteDetailResponse fromEntity(RecommendedRoute entity, String defaultModeLabel) {
        List<RouteWaypointResponse> waypoints = entity.getWaypoints().stream()
                .sorted(Comparator.comparing(RecommendedRouteWaypoint::getSortOrder).thenComparing(RecommendedRouteWaypoint::getId))
                .map(RouteWaypointResponse::fromEntity)
                .toList();
        return new AdminRecommendedRouteDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getDescription(),
                entity.getRecommendationText(),
                entity.getCoverImageUrl(),
                entity.getSortOrder(),
                entity.getDefaultMode(),
                defaultModeLabel,
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                waypoints
        );
    }
}
