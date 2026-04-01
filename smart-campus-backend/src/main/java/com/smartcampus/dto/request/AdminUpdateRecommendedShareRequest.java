package com.smartcampus.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateRecommendedShareRequest {

    @NotNull(message = "推荐排序不能为空")
    @Min(value = 1, message = "推荐排序必须大于等于 1")
    @Max(value = 999, message = "推荐排序不能超过 999")
    private Integer sortOrder;

    @Size(max = 100, message = "推荐语不能超过 100 个字符")
    private String recommendationText;
}
