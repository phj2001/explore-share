package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserItemResponse {

    private Long userId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private String bio;

    private LocalDateTime followedAt;
}
