package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileResponse {

    private Long userId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private String bio;

    private LocalDateTime createdAt;

    private long checkInCount;

    private long shareCount;

    private long receivedLikeCount;

    private long reviewCount;

    /** 账号状态（1 正常 / 2 已注销），前端据此展示「该用户已注销」并隐藏 @username */
    private Short status;

    /** 个人主页可见性：0 公开 / 1 仅关注者 / 2 仅自己（受限时前端据此区分两档占位文案） */
    private Short profileVisibility;

    /** 当前查看者是否可见其内容；false 时其余内容字段为空/0，前端渲染占位卡 */
    private boolean contentVisible;
}
