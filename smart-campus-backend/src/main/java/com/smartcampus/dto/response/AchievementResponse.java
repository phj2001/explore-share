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
}
