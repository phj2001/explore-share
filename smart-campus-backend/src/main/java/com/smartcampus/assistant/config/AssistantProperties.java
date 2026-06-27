package com.smartcampus.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 探索助手配置（前缀 app.assistant.*）。
 *
 * <p>遵循"独立模块 + 配置开关"铁律（任务规则 6）：{@link #enabled} 默认 false，
 * 关闭时 assistant 模块（ChatClient / 控制器 / 工具）整体不装配，已上线主功能零侵入，
 * 可一键回退为纯平台（方案 §13）。
 *
 * <p>各阶段字段按需引入：M0 仅 {@code enabled}；M2 起 {@code retrieval} 生效；
 * M4 起 cache / rateLimit 字段再补（避免提前过度设计，任务规则 8）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.assistant")
public class AssistantProperties {

    /**
     * 总开关：默认关闭。开启需显式 {@code app.assistant.enabled=true}。
     */
    private boolean enabled = false;

    /**
     * 空间约束混合检索参数（M2 起生效）。
     */
    private final Retrieval retrieval = new Retrieval();

    /** 语义缓存（M4，pgvector）。 */
    private final Cache cache = new Cache();

    /** 调用限流（M4，Redis）。 */
    private final RateLimit rateLimit = new RateLimit();

    /** 输入护栏（M4）。 */
    private final Guard guard = new Guard();

    @Data
    public static class Retrieval {

        /**
         * 空间预过滤半径（米）。复用 {@link com.smartcampus.util.GeoUtils#calculateBounds}
         * 计算边界框 —— 注意该方法半径单位为<b>千米</b>，调用前需 {@code /1000} 换算
         * （任务规则 4：复用现有 findWithinBounds 边界框，非 ST_DWithin）。
         */
        private int radiusMeters = 3000;

        /**
         * 候选集内语义 Top-K（默认 10）。
         */
        private int topK = 10;
    }

    @Data
    public static class Cache {
        /** 是否启用语义缓存。 */
        private boolean enabled = true;
        /** 命中阈值（余弦相似度 ≥ 此值视为命中）。 */
        private double similarityThreshold = 0.95;
        /** 缓存有效期（秒）。 */
        private long ttlSeconds = 86400;
    }

    @Data
    public static class RateLimit {
        /** 是否启用限流。 */
        private boolean enabled = true;
        /** 每用户每分钟最大对话次数。 */
        private int perUserPerMin = 20;
    }

    @Data
    public static class Guard {
        /** 用户消息最大长度。 */
        private int maxMessageLength = 500;
    }
}
