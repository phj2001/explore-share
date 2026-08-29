package com.smartcampus.service.impl;

import com.smartcampus.annotation.OperationLog;
import com.smartcampus.dto.request.ChangePasswordRequest;
import com.smartcampus.dto.request.UpdateUserProfileRequest;
import com.smartcampus.entity.User;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.security.ProfileVisibility;
import com.smartcampus.security.UserRole;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.EmailService;
import com.smartcampus.service.UserProfileService;
import com.smartcampus.service.storage.StorageCategory;
import com.smartcampus.service.storage.StorageService;
import com.smartcampus.util.RedisUtils;
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
    private final RedisUtils redisUtils;
    private final JwtTokenProvider jwtTokenProvider;
    private final StorageService storageService;
    private final EmailService emailService;

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
        // 隐私档位：null 不修改；非 null 必须是合法 code（不走 trimToNull 路径）
        if (request.getProfileVisibility() != null) {
            if (!ProfileVisibility.isValidCode(request.getProfileVisibility())) {
                throw new IllegalArgumentException("不支持的可见性档位");
            }
            user.setProfileVisibility(request.getProfileVisibility());
        }
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

        // 密码变更后吊销该用户所有旧 token，强制重新登录
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000 + 60;
        redisUtils.set("jwt:revoke_before:" + userId,
                String.valueOf(System.currentTimeMillis()),
                ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        redisUtils.delete("user:info:" + userId);
    }

    @Override
    @Transactional
    @OperationLog(module = "用户", action = "注销账号", targetType = "用户",
            targetIdSpel = "#userId", summarySpel = "'用户自主注销账号（匿名化保留内容） #' + #userId")
    public void deleteAccount(Long userId, String password) {
        User user = getRequiredUser(userId);

        // 站点唯一的超级管理员若自主注销将导致管理端失去控制权，禁止（与管理端“不能动超管”保护一致）
        if (user.getRole() == UserRole.SUPER_ADMIN.getCode()) {
            throw new IllegalArgumentException("超级管理员账号不支持自主注销");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码不正确");
        }

        // 匿名化前留存旧邮箱，注销成功后发送确认邮件（站内通知对已注销用户不可达）
        String oldEmail = user.getEmail();

        // 匿名化保留内容：用户行不删除，其发布的内容以「已注销用户」身份继续展示。
        // 密码改写为随机 UUID 密文，确保原密码此后永远无法登录。
        user.setUsername("deleted_user_" + userId);
        user.setEmail(null);
        user.setDisplayName("已注销用户");
        user.setBio(null);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.CANCELLED.getCode());
        deleteOldAvatar(user.getAvatarUrl());
        user.setAvatarUrl(null);
        userRepository.save(user);

        // 吊销该用户所有旧 token 并清缓存，强制立即下线（与 changePassword/logout 一致的安全闭环）
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000 + 60;
        redisUtils.set("jwt:revoke_before:" + userId,
                String.valueOf(System.currentTimeMillis()),
                ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        redisUtils.delete("user:info:" + userId);

        if (StringUtils.hasText(oldEmail)) {
            emailService.sendAccountDeletionNotice(oldEmail);
        }
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
        String avatarUrl = storageService.store(StorageCategory.AVATAR, filename, fileBytes);

        deleteOldAvatar(user.getAvatarUrl());
        user.setAvatarUrl(avatarUrl);
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

        storageService.delete(StorageCategory.AVATAR, filename);
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
