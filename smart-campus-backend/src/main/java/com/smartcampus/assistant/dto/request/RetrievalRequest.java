package com.smartcampus.assistant.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 空间约束混合检索请求（M2，方案 §9 入参）。
 *
 * <p>语义：在以 {@code (lat,lng)} 为中心、{@code radius} 米范围内，按 {@code query} 的语义
 * 召回 Top-K 相关 POI。{@code radius} 可选，缺省取 {@code app.assistant.retrieval.radius-meters}。
 */
@Data
public class RetrievalRequest {

    /** 自然语言问题（如"适合周末带娃的地方"）。 */
    @NotBlank(message = "问题不能为空")
    private String query;

    /** 中心纬度。 */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围为 -90~90")
    @DecimalMax(value = "90.0", message = "纬度范围为 -90~90")
    private Double lat;

    /** 中心经度。 */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围为 -180~180")
    @DecimalMax(value = "180.0", message = "经度范围为 -180~180")
    private Double lng;

    /** 可选半径（米），不传则用配置默认值。上限 100km 防止全表扫描。 */
    @Min(value = 1, message = "半径必须大于 0")
    @Max(value = 100000, message = "半径不能超过 100km")
    private Integer radius;
}
