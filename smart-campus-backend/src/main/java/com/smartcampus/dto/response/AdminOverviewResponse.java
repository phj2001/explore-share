package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResponse {

    private Integer rangeDays;
    private OverviewSummary summary;
    private List<OverviewTrendPoint> shareTrend;
    private List<OverviewHotPoi> hotPois;
    private List<OverviewRecentShare> recentShares;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewSummary {
        private Long poiCount;
        private Long userCount;
        private Long shareCount;
        private Long replyCount;
        private Long likeCount;
        private Long todayShareCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewTrendPoint {
        private String date;
        private Long value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewHotPoi {
        private Long poiId;
        private String poiName;
        private String category;
        private Long shareCount;
        private Long replyCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewRecentShare {
        private Long shareId;
        private Long poiId;
        private String poiName;
        private String authorDisplayName;
        private String authorUsername;
        private String authorAvatarUrl;
        private String contentPreview;
        private Long imageCount;
        private Long likeCount;
        private Long replyCount;
        private String createdAt;
    }
}
