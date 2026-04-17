package com.smartcampus.service.impl;

import com.smartcampus.dto.response.LoginResponse;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.security.UserRole;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.AuthService;
import com.smartcampus.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REDIS_REGISTER_PREFIX = "email_register:";
    private static final String REDIS_RESET_PREFIX    = "email_reset:";
    private static final long   CODE_TTL_SECONDS      = 300L; // 5 分钟

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    // ----------------------------- 注册（旧方法保持兼容）-----------------------------

    @Override
    @Transactional
    public User register(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(409, "邮箱已被注册，请使用其他邮箱");
        }
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ----------------------------- 注册（带邮箱验证）---------------------------

    @Override
    @Transactional
    public User register(User user, String emailCode) {
        // 校验邮箱验证码
        String key = REDIS_REGISTER_PREFIX + user.getEmail();
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(emailCode)) {
            throw new BusinessException(400, "验证码错误");
        }
        redisTemplate.delete(key);

        if (existsByUsername(user.getUsername())) {
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(409, "邮箱已被注册，请使用其他邮箱");
        }
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ----------------------------- 登录（支持用户名 / 邮箱）-----------------------

    @Override
    public Optional<LoginResponse> login(String identifier, String password) {
        // 先按用户名，查不到再按邮箱
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }
        return userOpt
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    if (UserStatus.fromCode(user.getStatus()) == UserStatus.DISABLED) {
                        throw new BusinessException(403, "账号已被禁用");
                    }
                    String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
                    return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
                });
    }

    @Override
    public Optional<User> verifyLogin(String identifier, String password) {
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }
        return userOpt
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .filter(user -> UserStatus.fromCode(user.getStatus()) == UserStatus.ACTIVE);
    }

    // ----------------------------- 邮箱验证码 -----------------------------------

    @Override
    public void sendRegisterCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(409, "邮箱已被注册");
        }
        String code = generateCode();
        redisTemplate.opsForValue().set(REDIS_REGISTER_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        emailService.sendRegisterCode(email, code);
    }

    @Override
    public void sendResetCode(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "该邮箱未绑定任何账号"));
        String code = generateCode();
        redisTemplate.opsForValue().set(REDIS_RESET_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        emailService.sendPasswordResetCode(email, code);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String key = REDIS_RESET_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误");
        }
        redisTemplate.delete(key);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ----------------------------- 其他查询 -------------------------------------

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ----------------------------- 登出 -----------------------------------------

    @Override
    public void logout(Long userId) {
        // 设置 revoke_before = 当前时间戳，JWT 签发时间早于此时间的 token 全部失效
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000 + 60;
        redisTemplate.opsForValue().set(
                "jwt:revoke_before:" + userId,
                String.valueOf(System.currentTimeMillis()),
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS
        );
        // 同步清除用户信息缓存
        redisTemplate.delete("user:info:" + userId);
    }

    // ----------------------------- 私有工具 -------------------------------------

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
