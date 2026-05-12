package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String type;

    private String title;

    private String content;

    private Long actorId;

    private String actorDisplayName;

    private String actorAvatarUrl;

    private String targetType;

    private Long targetId;

    private boolean read;

    private LocalDateTime createdAt;
}
