package com.smartcampus.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePOIReviewRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为 1 星")
    @Max(value = 5, message = "评分最高为 5 星")
    private Integer rating;

    @Size(max = 200, message = "评价内容不能超过 200 个字符")
    private String content;
}
