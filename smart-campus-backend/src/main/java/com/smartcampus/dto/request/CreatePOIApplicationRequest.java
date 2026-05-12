package com.smartcampus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePOIApplicationRequest {

    @NotBlank(message = "地点名称不能为空")
    @Size(max = 100, message = "地点名称不能超过100个字符")
    private String name;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类不能超过50个字符")
    private String category;

    @Size(max = 1000, message = "描述不能超过1000个字符")
    private String description;

    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    @Size(max = 255, message = "地址不能超过255个字符")
    private String address;

    private List<@Size(max = 255) String> photoUrls;
}
