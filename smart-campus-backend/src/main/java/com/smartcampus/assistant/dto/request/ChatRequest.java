package com.smartcampus.assistant.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 探索助手对话请求（M3，方案 §9）。
 *
 * <p>携带用户当前位置，助手据此调用工具做"地理围栏内的语义推荐 + 路线规划"。
 * 响应为 SSE 流（{@code POST /api/assistant/chat}）。
 */
@Data
public class ChatRequest {

    /** 用户自然语言消息（如"推荐附近适合周末带娃的地方"）。 */
    @NotBlank(message = "消息不能为空")
    private String message;

    /** 当前纬度。 */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围为 -90~90")
    @DecimalMax(value = "90.0", message = "纬度范围为 -90~90")
    private Double lat;

    /** 当前经度。 */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围为 -180~180")
    @DecimalMax(value = "180.0", message = "经度范围为 -180~180")
    private Double lng;

    /** 可选搜索半径（米），不传用配置默认值。上限 100km。 */
    @Min(value = 1, message = "半径必须大于 0")
    @Max(value = 100000, message = "半径不能超过 100km")
    private Integer radius;
}
