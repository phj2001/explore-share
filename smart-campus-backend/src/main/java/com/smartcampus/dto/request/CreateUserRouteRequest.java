package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRouteRequest {

    @NotBlank(message = "路线标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    @Size(max = 200, message = "摘要不能超过200个字符")
    private String summary;

    private String description;

    private String defaultMode;

    private String coverImageUrl;

    private List<WaypointInput> waypoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointInput {
        private Long poiId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String waypointName;
        private Integer sortOrder;
    }
}
