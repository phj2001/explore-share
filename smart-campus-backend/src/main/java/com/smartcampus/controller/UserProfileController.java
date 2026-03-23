package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.ChangePasswordRequest;
import com.smartcampus.dto.request.UpdateUserProfileRequest;
import com.smartcampus.dto.response.UserProfileResponse;
import com.smartcampus.entity.User;
import com.smartcampus.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public Result<UserProfileResponse> getCurrentProfile(Authentication authentication) {
        User user = userProfileService.getCurrentUserProfile(getCurrentUserId(authentication));
        return Result.success(UserProfileResponse.fromUser(user));
    }

    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        User user = userProfileService.updateProfile(getCurrentUserId(authentication), request);
        return Result.success(UserProfileResponse.fromUser(user));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(getCurrentUserId(authentication), request);
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<UserProfileResponse> uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        User user = userProfileService.updateAvatar(getCurrentUserId(authentication), file);
        return Result.success(UserProfileResponse.fromUser(user));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        return userId;
    }
}
