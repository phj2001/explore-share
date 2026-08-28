package com.smartcampus.dto.request;

import lombok.Data;

@Data
public class AdminUpdateSystemConfigRequest {

    /**
     * 不加 @NotBlank：文本类配置（如管理员联系方式）需要支持"清空"操作，
     * 空值的合法性由各配置项的 normalizer 判定（整数/布尔类 normalizer 对空值抛 400）。
     */
    private String value;
}
