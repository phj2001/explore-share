package com.smartcampus.service;

import com.smartcampus.dto.request.ChangePasswordRequest;
import com.smartcampus.dto.request.UpdateUserProfileRequest;
import com.smartcampus.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    User getCurrentUserProfile(Long userId);

    User updateProfile(Long userId, UpdateUserProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    User updateAvatar(Long userId, MultipartFile file);
}
