package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePOIShareReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 200, message = "回复内容不能超过200个字符")
    private String content;
}
