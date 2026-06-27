package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * AI 探索助手 · 调用限流（M4，方案 §8）。
 *
 * <p>基于 Redis 的"每用户每分钟"固定窗口计数，防止公开上线后被刷爆 LLM 成本。
 * 复用项目既有 Redis（与登录风控同套设施）。
 *
 * <p><b>容错（fail-open）</b>：Redis 异常时放行，避免缓存故障阻断正常对话。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final AssistantProperties properties;

    public AssistantRateLimiter(StringRedisTemplate redisTemplate, AssistantProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 是否允许本次调用。超过每分钟上限返回 false。
     *
     * @param userId 当前用户 ID（null 用 "anon"）
     */
    public boolean allow(Long userId) {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }
        int limit = properties.getRateLimit().getPerUserPerMin();
        long epochMinute = Instant.now().getEpochSecond() / 60;
        String key = "assistant:rl:" + (userId != null ? userId : "anon") + ":" + epochMinute;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(70));
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.warn("限流计数失败（放行）：{}", e.getMessage());
            return true;
        }
    }
}
