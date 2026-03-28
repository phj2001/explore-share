package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateSystemConfigRequest {

    @NotBlank(message = "配置值不能为空")
    private String value;
}
