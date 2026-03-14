package com.smartcampus.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class POIUpdateRequest {

    /**
     * POI名称
     */
    @Size(max = 100, message = "POI名称不能超过100个字符")
    private String name;

    /**
     * POI分类
     */
    @Size(max = 50, message = "POI分类不能超过50个字符")
    private String category;

    /**
     * POI描述
     */
    private String description;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;
}
