package com.smartcampus.security;

import com.smartcampus.entity.User;
import com.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.super-admin.username:}")
    private String superAdminUsername;

    @Value("${app.security.super-admin.password:}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN.getCode())) {
            return;
        }

        if (!StringUtils.hasText(superAdminUsername) || !StringUtils.hasText(superAdminPassword)) {
            log.warn("当前系统还没有超级管理员。请配置 SUPER_ADMIN_USERNAME 和 SUPER_ADMIN_PASSWORD，或手动将某个用户的 role 设置为 {}", UserRole.SUPER_ADMIN.getCode());
            return;
        }

        userRepository.findByUsername(superAdminUsername)
                .ifPresentOrElse(this::promoteToSuperAdmin, this::createSuperAdmin);
    }

    private void promoteToSuperAdmin(User user) {
        user.setRole(UserRole.SUPER_ADMIN.getCode());
        userRepository.save(user);
        log.info("已将用户 {} 提升为超级管理员", user.getUsername());
    }

    private void createSuperAdmin() {
        User user = new User();
        user.setUsername(superAdminUsername);
        user.setPassword(passwordEncoder.encode(superAdminPassword));
        user.setRole(UserRole.SUPER_ADMIN.getCode());
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("已创建初始超级管理员 {}", superAdminUsername);
    }
}
