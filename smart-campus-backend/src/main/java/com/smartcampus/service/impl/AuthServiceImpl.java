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
import com.smartcampus.service.LoginRiskControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REDIS_REGISTER_PREFIX = "email_register:";
    private static final String REDIS_RESET_PREFIX    = "email_reset:";
    private static final long   CODE_TTL_SECONDS      = 300L; // 5 分钟
    /** 同一验证码最多允许校验失败次数，超过即作废，防止 6 位数字码被暴力枚举 */
    private static final int    MAX_CODE_ATTEMPTS     = 5;
    /** 同一邮箱两次发送验证码的最小间隔（秒），防止接口被刷 / 邮件轰炸 */
    private static final long   SEND_INTERVAL_SECONDS = 60L;
    /** 验证码必须用密码学安全随机源生成（java.util.Random 种子可预测） */
    private static final SecureRandom SECURE_RANDOM   = new SecureRandom();

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final LoginRiskControlService loginRiskControlService;

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
        // 校验邮箱验证码（带失败次数限制，防暴力枚举）
        verifyEmailCode(REDIS_REGISTER_PREFIX + user.getEmail(), emailCode);

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
        // 前置风控：账号锁定则直接拒绝（升级项③）
        loginRiskControlService.assertLoginAllowed(identifier);

        // 先按用户名，查不到再按邮箱
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }

        if (userOpt.isEmpty()) {
            // 用户不存在也记失败，防止用户名枚举探测
            loginRiskControlService.recordFailure(identifier);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 密码错误记失败
            loginRiskControlService.recordFailure(identifier);
            return Optional.empty();
        }

        // 密码正确即清零失败计数——合法用户凭据已验证（即使后续因账号禁用抛异常，也先清计数）
        loginRiskControlService.clearFailures(identifier);

        if (UserStatus.fromCode(user.getStatus()) == UserStatus.DISABLED) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Optional.of(new LoginResponse(token, user.getId(), user.getUsername(), user.getRole()));
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
        assertSendAllowed(REDIS_REGISTER_PREFIX + email);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(409, "邮箱已被注册");
        }
        String code = generateCode();
        redisTemplate.opsForValue().set(REDIS_REGISTER_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        emailService.sendRegisterCode(email, code);
    }

    @Override
    public void sendResetCode(String email) {
        assertSendAllowed(REDIS_RESET_PREFIX + email);
        userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "该邮箱未绑定任何账号"));
        String code = generateCode();
        redisTemplate.opsForValue().set(REDIS_RESET_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        emailService.sendPasswordResetCode(email, code);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // 校验邮箱验证码（带失败次数限制，防暴力枚举——重置密码是账号接管的直接入口，必须限次）
        verifyEmailCode(REDIS_RESET_PREFIX + email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 密码重置后吊销该用户所有旧 token 并清缓存，强制重新登录（与 changePassword/logout 一致的安全闭环）：
        // 否则账号被盗后用户走“找回密码”改回密码，攻击者持有的旧 JWT 在最长 jwt.expiration 内仍可继续访问
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000 + 60;
        redisTemplate.opsForValue().set(
                "jwt:revoke_before:" + user.getId(),
                String.valueOf(System.currentTimeMillis()),
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS
        );
        redisTemplate.delete("user:info:" + user.getId());
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
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    /**
     * 校验邮箱验证码，带失败次数限制：
     * 同一验证码累计校验失败 {@value MAX_CODE_ATTEMPTS} 次即作废（删除验证码），必须重新获取。
     * 防止攻击者在 5 分钟 TTL 内穷举 6 位数字码（重置密码场景 = 账号接管入口）。
     * 校验通过后同时清除验证码与失败计数。
     *
     * @param codeKey Redis 中验证码的完整 key（前缀 + 邮箱）
     * @param inputCode 用户提交的验证码
     */
    private void verifyEmailCode(String codeKey, String inputCode) {
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!storedCode.equals(inputCode)) {
            String attemptsKey = codeKey + ":attempts";
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1L) {
                // 计数器与验证码同生命周期
                redisTemplate.expire(attemptsKey, Duration.ofSeconds(CODE_TTL_SECONDS));
            }
            if (attempts != null && attempts >= MAX_CODE_ATTEMPTS) {
                redisTemplate.delete(codeKey);
                redisTemplate.delete(attemptsKey);
                throw new BusinessException(400, "验证码错误次数过多，已失效，请重新获取");
            }
            throw new BusinessException(400, "验证码错误");
        }
        redisTemplate.delete(codeKey);
        redisTemplate.delete(codeKey + ":attempts");
    }

    /**
     * 发送频率限制：同一邮箱 {@value SEND_INTERVAL_SECONDS} 秒内只允许发送一次验证码，
     * 防止发送接口被刷（邮件轰炸 / SMTP 配额消耗）。
     *
     * @param codeKey Redis 中验证码的完整 key（前缀 + 邮箱），锁 key 在其基础上派生
     */
    private void assertSendAllowed(String codeKey) {
        String lockKey = codeKey + ":send_lock";
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(SEND_INTERVAL_SECONDS));
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(429, "验证码发送过于频繁，请 " + SEND_INTERVAL_SECONDS + " 秒后再试");
        }
    }
}
