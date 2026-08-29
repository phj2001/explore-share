package com.smartcampus.dto.response;

import com.smartcampus.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String email;
    private Short role;
    private Short status;
    private Short profileVisibility;
    private Boolean canResetPassword;

    public static UserProfileResponse fromUser(User user) {
        UserProfileResponse r = new UserProfileResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setDisplayName(user.getDisplayName());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setBio(user.getBio());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());
        r.setStatus(user.getStatus());
        r.setProfileVisibility(user.getProfileVisibility());
        r.setCanResetPassword(Boolean.TRUE.equals(user.getCanResetPassword()));
        return r;
    }
}
