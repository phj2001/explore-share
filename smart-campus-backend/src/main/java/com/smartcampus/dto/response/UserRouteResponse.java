package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteResponse {

    private Long id;

    private Long userId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private String title;

    private String summary;

    private String description;

    private String defaultMode;

    private String coverImageUrl;

    private int likeCount;

    private int favoriteCount;

    private boolean liked;

    private boolean favorited;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<WaypointResponse> waypoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointResponse {
        private Long id;
        private Long poiId;
        private String poiName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String waypointName;
        private Integer sortOrder;
    }
}
