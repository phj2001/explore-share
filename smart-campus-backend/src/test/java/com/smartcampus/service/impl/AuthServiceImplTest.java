package com.smartcampus.service.impl;

import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.security.UserRole;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.EmailService;
import com.smartcampus.service.LoginRiskControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 注册邮箱选填化单元测试。
 * 纯 Mockito，mock StringRedisTemplate / UserRepository，不依赖 Redis 与数据库实例。
 */
class AuthServiceImplTest {

    private static final String CODE_KEY = "email_register:a@b.com";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private AuthServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked") // mock 泛型类（ValueOperations）因类型擦除必然产生 unchecked 警告，此处安全
    void setUp() {
        userRepository = mock(UserRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        EmailService emailService = mock(EmailService.class);
        LoginRiskControlService loginRiskControlService = mock(LoginRiskControlService.class);
        service = new AuthServiceImpl(userRepository, jwtTokenProvider, passwordEncoder,
                redisTemplate, emailService, loginRiskControlService);
    }

    private User buildRegisterUser(String email) {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("rawPass123");
        user.setEmail(email);
        return user;
    }

    @Test
    void register_未填邮箱_直接注册成功且不校验验证码() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register(buildRegisterUser(null), null);

        assertNull(saved.getEmail());
        assertEquals("encoded", saved.getPassword());
        assertEquals(UserRole.USER.getCode(), saved.getRole());
        assertEquals(UserStatus.ACTIVE.getCode(), saved.getStatus());
        verify(userRepository, never()).existsByEmail(anyString());
        // 完全不触碰验证码 Redis
        verifyNoInteractions(valueOps);
    }

    @Test
    void register_邮箱为空白字符串_按未填邮箱处理() {
        // 回归防护：空串会误入邮箱分支，且 email 部分唯一索引把 '' 当已占用值
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register(buildRegisterUser("   "), "123456");

        assertNull(saved.getEmail());
        verify(userRepository, never()).existsByEmail(anyString());
        verifyNoInteractions(valueOps);
    }

    @Test
    void register_填了邮箱但缺验证码_抛400() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(buildRegisterUser("a@b.com"), "  "));

        assertEquals(400, ex.getCode());
        assertEquals("请输入邮箱验证码", ex.getMessage());
        verifyNoInteractions(valueOps);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_验证码错误_抛400并计失败次数() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(valueOps.get(CODE_KEY)).thenReturn("111111");
        when(valueOps.increment(CODE_KEY + ":attempts")).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(buildRegisterUser("a@b.com"), "222222"));

        assertEquals(400, ex.getCode());
        assertEquals("验证码错误", ex.getMessage());
        verify(valueOps).increment(CODE_KEY + ":attempts");
        // 计数器与验证码同生命周期
        verify(redisTemplate).expire(eq(CODE_KEY + ":attempts"), any(Duration.class));
        // 验证码未被作废、用户未被保存
        verify(redisTemplate, never()).delete(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_验证码正确_注册成功并删除验证码() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(valueOps.get(CODE_KEY)).thenReturn("123456");
        when(passwordEncoder.encode("rawPass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register(buildRegisterUser("a@b.com"), "123456");

        assertEquals("a@b.com", saved.getEmail());
        assertEquals("encoded", saved.getPassword());
        verify(redisTemplate).delete(CODE_KEY);
        verify(redisTemplate).delete(CODE_KEY + ":attempts");
    }

    @Test
    void register_邮箱已被注册_先于验码抛409且不消耗验证码() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(buildRegisterUser("a@b.com"), "123456"));

        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("邮箱已被注册"));
        // 邮箱重复在验码之前拦截：用户已获取的有效验证码不被烧掉
        verifyNoInteractions(valueOps);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_用户名重复_抛409且不进邮箱分支() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(buildRegisterUser("a@b.com"), "123456"));

        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).existsByEmail(anyString());
        verifyNoInteractions(valueOps);
        verify(userRepository, never()).save(any(User.class));
    }
}
