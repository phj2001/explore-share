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
    private Short role;

    public static UserProfileResponse fromUser(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
