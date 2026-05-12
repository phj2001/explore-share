package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRouteListItemResponse {

    private Long id;

    private Long userId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private String title;

    private String summary;

    private String defaultMode;

    private String coverImageUrl;

    private int likeCount;

    private int favoriteCount;

    private int waypointCount;

    private LocalDateTime createdAt;
}
