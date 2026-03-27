package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "状态不能为空")
    private Short status;
}
