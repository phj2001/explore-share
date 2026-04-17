package com.smartcampus.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ===== 基础 String 操作 =====

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ===== 对象序列化（JSON）=====

    public <T> void setObject(String key, T value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, timeout, unit);
        } catch (Exception e) {
            log.error("Redis setObject 序列化失败, key={}", key, e);
        }
    }

    public <T> T getObject(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Redis getObject 反序列化失败, key={}", key, e);
            return null;
        }
    }

    /**
     * 获取 List 类型缓存（Jackson TypeReference 方式）
     */
    public <T> List<T> getList(String key, Class<T> elementClass) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Redis getList 反序列化失败, key={}", key, e);
            return null;
        }
    }

    // ===== 滑动窗口限流（Lua 脚本原子操作）=====

    /**
     * @param key           限流 key
     * @param windowSeconds 时间窗口（秒）
     * @param maxCount      窗口内最大请求数
     * @return true=放行，false=限流
     */
    public boolean isAllowed(String key, int windowSeconds, int maxCount) {
        String script = """
                local key = KEYS[1]
                local window = tonumber(ARGV[1])
                local maxCount = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                local windowStart = now - window * 1000
                redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
                local count = redis.call('ZCARD', key)
                if count < maxCount then
                    redis.call('ZADD', key, now, now)
                    redis.call('EXPIRE', key, window)
                    return 1
                else
                    return 0
                end
                """;
        Long result = redisTemplate.execute(
                RedisScript.of(script, Long.class),
                List.of(key),
                String.valueOf(windowSeconds),
                String.valueOf(maxCount),
                String.valueOf(System.currentTimeMillis())
        );
        return result != null && result == 1L;
    }
}
