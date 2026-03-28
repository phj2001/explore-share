package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReviewContentReportRequest {

    @NotNull(message = "处理状态不能为空")
    private Short status;

    private Short action;

    private String reviewNote;
}
