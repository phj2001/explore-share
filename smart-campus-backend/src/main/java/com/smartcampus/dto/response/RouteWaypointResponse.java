package com.smartcampus.dto.response;

import com.smartcampus.entity.POI;
import com.smartcampus.entity.RecommendedRouteWaypoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteWaypointResponse {

    private Long poiId;

    private String poiName;

    private String poiCategory;

    private String poiDescription;

    private Double latitude;

    private Double longitude;

    private Integer sortOrder;

    public static RouteWaypointResponse fromEntity(RecommendedRouteWaypoint waypoint) {
        POI poi = waypoint.getPoi();
        return new RouteWaypointResponse(
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getDescription(),
                poi.getLatitude() == null ? null : poi.getLatitude().doubleValue(),
                poi.getLongitude() == null ? null : poi.getLongitude().doubleValue(),
                waypoint.getSortOrder()
        );
    }
}
