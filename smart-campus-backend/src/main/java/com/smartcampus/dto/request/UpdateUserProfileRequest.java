package com.smartcampus.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @Size(max = 100, message = "展示名不能超过100个字符")
    private String displayName;

    @Size(max = 150, message = "个性签名不能超过150个字符")
    private String bio;

    /** 个人主页可见性：0 公开 / 1 仅关注者 / 2 仅自己；null 表示不修改 */
    private Short profileVisibility;
}
