package com.smartcampus.dto.response;

import com.smartcampus.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListItemResponse {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private Short role;
    private Short status;
    private LocalDateTime createdAt;

    public static AdminUserListItemResponse fromUser(User user) {
        return new AdminUserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
