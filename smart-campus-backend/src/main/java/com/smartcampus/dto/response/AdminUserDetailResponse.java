package com.smartcampus.dto.response;

import com.smartcampus.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private Short role;
    private Short status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long shareCount;
    private Long replyCount;
    private Long likeCount;

    public static AdminUserDetailResponse fromUser(User user, Long shareCount, Long replyCount, Long likeCount) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                shareCount,
                replyCount,
                likeCount
        );
    }
}
