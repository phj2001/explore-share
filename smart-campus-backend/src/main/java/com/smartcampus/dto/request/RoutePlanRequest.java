package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoutePlanRequest {

    /**
     * 起点纬度
     */
    @NotNull(message = "起点纬度不能为空")
    private BigDecimal startLat;

    /**
     * 起点经度
     */
    @NotNull(message = "起点经度不能为空")
    private BigDecimal startLng;

    /**
     * 终点纬度
     */
    @NotNull(message = "终点纬度不能为空")
    private BigDecimal endLat;

    /**
     * 终点经度
     */
    @NotNull(message = "终点经度不能为空")
    private BigDecimal endLng;
}
