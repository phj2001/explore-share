package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminRenamePoiCategoryRequest {

    @NotBlank(message = "新分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String newName;
}
