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
public class RecommendedRouteListItemResponse {

    private Long id;

    private String title;

    private String summary;

    private String recommendationText;

    private String coverImageUrl;

    private Integer sortOrder;

    private String defaultMode;

    private String defaultModeLabel;

    private Integer waypointCount;

    private String startPoiName;

    private String endPoiName;

    public static RecommendedRouteListItemResponse fromEntity(RecommendedRoute entity, String defaultModeLabel) {
        List<RecommendedRouteWaypoint> sortedWaypoints = entity.getWaypoints().stream()
                .sorted(Comparator.comparing(RecommendedRouteWaypoint::getSortOrder).thenComparing(RecommendedRouteWaypoint::getId))
                .toList();
        String startPoiName = sortedWaypoints.isEmpty() ? null : sortedWaypoints.get(0).getPoi().getName();
        String endPoiName = sortedWaypoints.isEmpty() ? null : sortedWaypoints.get(sortedWaypoints.size() - 1).getPoi().getName();
        return new RecommendedRouteListItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getRecommendationText(),
                entity.getCoverImageUrl(),
                entity.getSortOrder(),
                entity.getDefaultMode(),
                defaultModeLabel,
                sortedWaypoints.size(),
                startPoiName,
                endPoiName
        );
    }
}
