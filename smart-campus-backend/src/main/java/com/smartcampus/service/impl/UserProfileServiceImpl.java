package com.smartcampus.service.impl;

import com.smartcampus.dto.request.ChangePasswordRequest;
import com.smartcampus.dto.request.UpdateUserProfileRequest;
import com.smartcampus.entity.User;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.UserProfileService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarUploadDir;

    private Path avatarStoragePath;

    @PostConstruct
    public void init() throws IOException {
        avatarStoragePath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(avatarStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUserProfile(Long userId) {
        return getRequiredUser(userId);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getRequiredUser(userId);
        user.setDisplayName(trimToNull(request.getDisplayName()));
        user.setBio(trimToNull(request.getBio()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getRequiredUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的头像");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("头像读取失败");
        }

        if (fileBytes.length > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像大小不能超过2MB");
        }

        String extension = detectImageExtension(fileBytes);
        if (extension == null) {
            throw new IllegalArgumentException("头像仅支持 JPG、PNG、WEBP 格式");
        }

        User user = getRequiredUser(userId);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = avatarStoragePath.resolve(filename).normalize();

        if (!targetPath.startsWith(avatarStoragePath)) {
            throw new IllegalArgumentException("非法的头像存储路径");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("头像保存失败", e);
        }

        deleteOldAvatar(user.getAvatarUrl());
        user.setAvatarUrl(AVATAR_URL_PREFIX + filename);
        return userRepository.save(user);
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void deleteOldAvatar(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }

        String filename = avatarUrl.substring(AVATAR_URL_PREFIX.length());
        if (!StringUtils.hasText(filename)) {
            return;
        }

        Path oldFile = avatarStoragePath.resolve(filename).normalize();
        if (!oldFile.startsWith(avatarStoragePath)) {
            return;
        }

        try {
            Files.deleteIfExists(oldFile);
        } catch (IOException ignored) {
        }
    }

    private String detectImageExtension(byte[] bytes) {
        if (isPng(bytes)) {
            return "png";
        }
        if (isJpeg(bytes)) {
            return "jpg";
        }
        if (isWebp(bytes)) {
            return "webp";
        }
        return null;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && readAscii(bytes, 0, 4).equals("RIFF")
                && readAscii(bytes, 8, 4).equals("WEBP");
    }

    private String readAscii(byte[] bytes, int start, int length) {
        return new String(bytes, start, length).toUpperCase(Locale.ROOT);
    }
}
