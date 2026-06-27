package com.smartcampus.service.impl;

import com.smartcampus.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 登录风控单元测试（升级项③ 回归）。
 * 纯 Mockito，mock StringRedisTemplate，不依赖 Redis 实例。
 */
class LoginRiskControlServiceImplTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private LoginRiskControlServiceImpl service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        service = new LoginRiskControlServiceImpl(provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxFailures", 5);
        ReflectionTestUtils.setField(service, "lockDurationSeconds", 900L);
        ReflectionTestUtils.setField(service, "counterTtlSeconds", 1800L);
    }

    @Test
    void assertLoginAllowed_未锁定_直接通过() {
        when(valueOps.get("auth:login:fail:lock:user1")).thenReturn(null);
        assertDoesNotThrow(() -> service.assertLoginAllowed("user1"));
    }

    @Test
    void assertLoginAllowed_已锁定_抛429异常含剩余秒数() {
        long lockedUntil = System.currentTimeMillis() + 60_000;
        when(valueOps.get("auth:login:fail:lock:user1")).thenReturn(String.valueOf(lockedUntil));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertLoginAllowed("user1"));
        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("秒后重试"));
    }

    @Test
    void assertLoginAllowed_锁定已过期_通过() {
        long lockedUntil = System.currentTimeMillis() - 1_000;
        when(valueOps.get("auth:login:fail:lock:user1")).thenReturn(String.valueOf(lockedUntil));
        assertDoesNotThrow(() -> service.assertLoginAllowed("user1"));
    }

    @Test
    void recordFailure_不足阈值_不设置锁() {
        when(valueOps.increment("auth:login:fail:count:user1")).thenReturn(3L);
        service.recordFailure("user1");
        verify(valueOps).increment("auth:login:fail:count:user1");
        verify(valueOps, never()).set(eq("auth:login:fail:lock:user1"), anyString(), any(Duration.class));
    }

    @Test
    void recordFailure_达到阈值_设置锁() {
        when(valueOps.increment("auth:login:fail:count:user1")).thenReturn(5L);
        service.recordFailure("user1");
        verify(valueOps).set(eq("auth:login:fail:lock:user1"), anyString(), any(Duration.class));
    }

    @Test
    void clearFailures_删除count和lock两个key() {
        service.clearFailures("user1");
        verify(redisTemplate).delete("auth:login:fail:count:user1");
        verify(redisTemplate).delete("auth:login:fail:lock:user1");
    }

    @Test
    void 降级_Redis不可用_走本地内存仍能锁定() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        LoginRiskControlServiceImpl localService = new LoginRiskControlServiceImpl(emptyProvider);
        ReflectionTestUtils.setField(localService, "enabled", true);
        ReflectionTestUtils.setField(localService, "maxFailures", 3);
        ReflectionTestUtils.setField(localService, "lockDurationSeconds", 60L);
        ReflectionTestUtils.setField(localService, "counterTtlSeconds", 120L);

        // 未锁定通过
        assertDoesNotThrow(() -> localService.assertLoginAllowed("user2"));
        // 累计失败达阈值（3 次）
        localService.recordFailure("user2");
        localService.recordFailure("user2");
        localService.recordFailure("user2");
        // 第 4 次断言应被锁定
        BusinessException ex = assertThrows(BusinessException.class,
                () -> localService.assertLoginAllowed("user2"));
        assertEquals(429, ex.getCode());
    }

    @Test
    void 开关关闭_所有方法空操作不触发Redis() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.recordFailure("user1");
        service.clearFailures("user1");
        assertDoesNotThrow(() -> service.assertLoginAllowed("user1"));
        verifyNoInteractions(valueOps);
    }

    @Test
    void 空用户名_所有方法空操作() {
        service.recordFailure("");
        service.clearFailures("   ");
        assertDoesNotThrow(() -> service.assertLoginAllowed(null));
        verifyNoInteractions(valueOps);
        // increment 从未被调用
        verify(valueOps, times(0)).increment(anyString());
    }
}
