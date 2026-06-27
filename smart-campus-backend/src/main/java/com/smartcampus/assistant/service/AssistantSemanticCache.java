package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * AI 探索助手 · 语义缓存（M4，方案 §8）。
 *
 * <p>用 pgvector 实现"相似问题命中"：把问题向量按地理分桶存储，新问题在同桶内做余弦近邻，
 * 相似度 ≥ 阈值即复用历史回答，显著降低 LLM/工具调用成本与延迟。
 *
 * <p><b>容错（fail-open）</b>：缓存查询/写入的任何异常都不影响主对话——查询异常视为未命中，
 * 写入异常仅记日志。即使 assistant_cache 表尚未创建（既有库未跑 03 脚本），也不会中断对话。
 *
 * <p>回答不含用户个性化信息（M3 未注入用户历史），故缓存可跨用户共享。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantSemanticCache {

    private final JdbcTemplate jdbcTemplate;
    private final OpenAiEmbeddingModel embeddingModel;
    private final AssistantProperties properties;

    public AssistantSemanticCache(JdbcTemplate jdbcTemplate,
                                  OpenAiEmbeddingModel embeddingModel,
                                  AssistantProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    /** 查询缓存命中（未启用 / 未命中 / 异常 → 空）。 */
    public Optional<String> lookup(String message, double lat, double lng) {
        if (!properties.getCache().isEnabled()) {
            return Optional.empty();
        }
        try {
            float[] vec = embeddingModel.embed(message);
            String bucket = locBucket(lat, lng);
            double threshold = properties.getCache().getSimilarityThreshold();
            long ttl = properties.getCache().getTtlSeconds();
            String literal = toVectorLiteral(vec);

            List<Hit> hits = jdbcTemplate.query(
                    "SELECT response, 1 - (embedding <=> ?::vector) AS score " +
                            "FROM assistant_cache " +
                            "WHERE loc_bucket = ? AND created_at >= now() - (? || ' seconds')::interval " +
                            "ORDER BY embedding <=> ?::vector LIMIT 1",
                    (rs, i) -> new Hit(rs.getString("response"), rs.getDouble("score")),
                    literal, bucket, ttl, literal);

            if (!hits.isEmpty() && hits.get(0).score() >= threshold) {
                return Optional.of(hits.get(0).response());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("语义缓存查询失败（视为未命中）：{}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 写入缓存（未启用 / 异常 → 静默忽略）。 */
    public void put(String message, double lat, double lng, String response) {
        if (!properties.getCache().isEnabled() || response == null || response.isBlank()) {
            return;
        }
        try {
            float[] vec = embeddingModel.embed(message);
            jdbcTemplate.update(
                    "INSERT INTO assistant_cache (loc_bucket, query, embedding, response) VALUES (?, ?, ?::vector, ?)",
                    locBucket(lat, lng), message, toVectorLiteral(vec), response);
        } catch (Exception e) {
            log.warn("语义缓存写入失败（忽略）：{}", e.getMessage());
        }
    }

    /** 经纬度粗粒度分桶（保留 3 位小数 ≈ 110m），缓存按区域隔离。 */
    private static String locBucket(double lat, double lng) {
        return String.format("%.3f,%.3f", lat, lng);
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 8).append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    private record Hit(String response, double score) {}
}
