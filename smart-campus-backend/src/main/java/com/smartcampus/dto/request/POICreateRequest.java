package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class POICreateRequest {

    /**
     * POI名称
     */
    @NotBlank(message = "POI名称不能为空")
    @Size(max = 100, message = "POI名称不能超过100个字符")
    private String name;

    /**
     * POI分类
     */
    @NotBlank(message = "POI分类不能为空")
    @Size(max = 50, message = "POI分类不能超过50个字符")
    private String category;

    /**
     * POI描述
     */
    private String description;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;
}
