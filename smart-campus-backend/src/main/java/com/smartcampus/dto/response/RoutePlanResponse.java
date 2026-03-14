package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanResponse {

    /**
     * 起点纬度
     */
    private BigDecimal startLat;

    /**
     * 起点经度
     */
    private BigDecimal startLng;

    /**
     * 终点纬度
     */
    private BigDecimal endLat;

    /**
     * 终点经度
     */
    private BigDecimal endLng;

    /**
     * 路径总距离（单位：千米）
     */
    private Double distance;

    /**
     * 路径上的POI点列表
     */
    private List<POIResponse> waypoints;

    /**
     * 预估时间（单位：分钟）
     */
    private Integer estimatedTime;
}
