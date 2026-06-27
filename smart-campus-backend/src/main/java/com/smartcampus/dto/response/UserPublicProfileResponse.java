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
}
