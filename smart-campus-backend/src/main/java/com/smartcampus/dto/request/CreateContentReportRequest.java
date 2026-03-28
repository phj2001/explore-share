package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateContentReportRequest {

    @NotNull(message = "举报理由不能为空")
    private Short reasonCode;

    private String reasonDetail;
}
