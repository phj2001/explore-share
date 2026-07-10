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

    /** 多轮对话记忆（M7，升级方案 P1）。 */
    private final History history = new History();

    /** 个性化推荐（M7，升级方案 P2）。 */
    private final Personalization personalization = new Personalization();

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
        /** 过期行物理清理间隔（毫秒，默认 1 小时）；TTL 只在查询时过滤，需定时删除防表膨胀。 */
        private long cleanupIntervalMs = 3600000;
        /** 清理任务首轮延迟（毫秒，默认 1 分钟），避开应用启动高峰。 */
        private long cleanupInitialDelayMs = 60000;
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

    @Data
    public static class History {
        /** 是否启用多轮对话记忆；关闭时行为退回 M3 单轮模式。 */
        private boolean enabled = true;
        /** 组装 prompt 时携带的最近历史轮次数（一问一答算 1 轮），避免 token 无限增长。 */
        private int maxTurns = 6;
        /** 会话在 Redis 中的存活时间（分钟），超时未续期自动过期，无需显式清理接口。 */
        private long ttlMinutes = 30;
    }

    @Data
    public static class Personalization {
        /** 是否启用个性化工具（getUserPreferences）。 */
        private boolean enabled = true;
        /** 汇总用户偏好时各来源（收藏/签到）各自读取的最大记录数。 */
        private int maxRecords = 20;
    }
}
