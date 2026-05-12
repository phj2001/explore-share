package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewPOIApplicationRequest {

    @NotNull(message = "审核状态不能为空")
    private Short status;

    @Size(max = 500, message = "审核意见不能超过500个字符")
    private String reviewNote;
}
