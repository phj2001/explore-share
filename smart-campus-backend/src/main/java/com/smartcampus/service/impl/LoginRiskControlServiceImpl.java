package com.smartcampus.service.impl;

import com.smartcampus.exception.BusinessException;
import com.smartcampus.service.LoginRiskControlService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录风控实现（升级项③）
 *
 * <p>来源：智慧校园 LoginRiskControlServiceImpl。保留 Redis 优先 + ConcurrentHashMap 本地降级的双保险设计
 * （Redis 不可用时不阻断业务）。唯一适配点：assertLoginAllowed 抛 {@link BusinessException}(429)
 * 取代原 RuntimeException——discover 统一用 BusinessException 表达业务错误，429 语义=Too Many Requests，
 * 经 GlobalExceptionHandler.handleBusinessException 返回 Result{code:429}。
 */
@Service
public class LoginRiskControlServiceImpl implements LoginRiskControlService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.login-risk-control-enabled:true}")
    private boolean enabled;

    @Value("${app.security.login-max-failures:5}")
    private int maxFailures;

    @Value("${app.security.login-lock-duration-seconds:900}")
    private long lockDurationSeconds;

    @Value("${app.security.login-counter-ttl-seconds:1800}")
    private long counterTtlSeconds;

    private final Map<String, Integer> localFailureCount = new ConcurrentHashMap<>();
    private final Map<String, Instant> localFailureExpiry = new ConcurrentHashMap<>();
    private final Map<String, Instant> localLockedUntil = new ConcurrentHashMap<>();

    public LoginRiskControlServiceImpl(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        // ObjectProvider.getIfAvailable：Redis 未配置时 redisTemplate=null，自动降级到本地内存
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Override
    public void assertLoginAllowed(String username) {
        if (!enabled || !StringUtils.hasText(username)) {
            return;
        }
        String normalized = normalize(username);
        Instant lockedUntil = resolveLockedUntil(normalized);
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            long remainingSeconds = Duration.between(Instant.now(), lockedUntil).toSeconds();
            throw new BusinessException(429, "登录失败次数过多，请于 " + Math.max(1, remainingSeconds) + " 秒后重试");
        }
        cleanupLocalState(normalized);
    }

    @Override
    public void recordFailure(String username) {
        if (!enabled || !StringUtils.hasText(username)) {
            return;
        }
        String normalized = normalize(username);
        if (redisTemplate != null) {
            try {
                String countKey = buildCountKey(normalized);
                Long count = redisTemplate.opsForValue().increment(countKey);
                redisTemplate.expire(countKey, Duration.ofSeconds(counterTtlSeconds));
                if (count != null && count >= maxFailures) {
                    Instant lockedUntil = Instant.now().plusSeconds(lockDurationSeconds);
                    String lockKey = buildLockKey(normalized);
                    redisTemplate.opsForValue().set(lockKey, String.valueOf(lockedUntil.toEpochMilli()), Duration.ofSeconds(lockDurationSeconds));
                }
                return;
            } catch (Exception ignored) {
                // Redis 不可用时降级到本地内存
            }
        }
        cleanupLocalState(normalized);
        int nextCount = localFailureCount.getOrDefault(normalized, 0) + 1;
        localFailureCount.put(normalized, nextCount);
        localFailureExpiry.put(normalized, Instant.now().plusSeconds(counterTtlSeconds));
        if (nextCount >= maxFailures) {
            localLockedUntil.put(normalized, Instant.now().plusSeconds(lockDurationSeconds));
        }
    }

    @Override
    public void clearFailures(String username) {
        if (!enabled || !StringUtils.hasText(username)) {
            return;
        }
        String normalized = normalize(username);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(buildCountKey(normalized));
                redisTemplate.delete(buildLockKey(normalized));
                return;
            } catch (Exception ignored) {
                // Redis 不可用时降级到本地内存
            }
        }
        localFailureCount.remove(normalized);
        localFailureExpiry.remove(normalized);
        localLockedUntil.remove(normalized);
    }

    private Instant resolveLockedUntil(String username) {
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(buildLockKey(username));
                if (!StringUtils.hasText(value)) {
                    return null;
                }
                return Instant.ofEpochMilli(Long.parseLong(value));
            } catch (Exception ignored) {
                // Redis 不可用时降级到本地内存
            }
        }
        return localLockedUntil.get(username);
    }

    private void cleanupLocalState(String username) {
        Instant now = Instant.now();
        Instant expiry = localFailureExpiry.get(username);
        if (expiry != null && !expiry.isAfter(now)) {
            localFailureCount.remove(username);
            localFailureExpiry.remove(username);
        }
        Instant lockedUntil = localLockedUntil.get(username);
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            localLockedUntil.remove(username);
        }
    }

    /**
     * 定时清理本地降级缓存中的过期条目（修复 #7）。
     *
     * <p>{@code cleanupLocalState} 只清"当次访问到的用户名"，Redis 宕机降级期间若遭遇随机用户名
     * 攻击，未再访问的陈旧条目会长期滞留导致内存缓慢增长。此处周期性全量清理过期项兜底。
     * 仅作用于本地内存（Redis 正常时这些 Map 本就为空）。依赖主启动类 @EnableScheduling。
     */
    @Scheduled(fixedDelayString = "${app.security.login-risk-local-cleanup-ms:600000}")
    public void purgeExpiredLocalState() {
        Instant now = Instant.now();
        localFailureExpiry.forEach((key, expiry) -> {
            if (expiry == null || !expiry.isAfter(now)) {
                localFailureExpiry.remove(key);
                localFailureCount.remove(key);
            }
        });
        localLockedUntil.forEach((key, lockedUntil) -> {
            if (lockedUntil == null || !lockedUntil.isAfter(now)) {
                localLockedUntil.remove(key);
            }
        });
    }

    private String buildCountKey(String username) {
        return "auth:login:fail:count:" + username;
    }

    private String buildLockKey(String username) {
        return "auth:login:fail:lock:" + username;
    }

    private String normalize(String username) {
        return username.trim();
    }
}
