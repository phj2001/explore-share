package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementResponse {

    private String id;

    private String name;

    private String description;

    private String iconUrl;

    private String category;

    private int sortOrder;

    private boolean unlocked;

    private LocalDateTime unlockedAt;

    /** 进度当前值（已解锁或无规则的成就为 null）；展示时封顶于阈值 */
    private Long progressCurrent;

    /** 进度阈值（无规则的成就为 null） */
    private Long progressTarget;
}
