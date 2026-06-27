package com.smartcampus.assistant.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * POI 向量表访问层（poi_embedding）。
 *
 * <p>不交给 JPA：vector 列 Hibernate 不识别（方案 §6.1），改用 JdbcTemplate 原生 SQL，
 * float[] 与 vector 列之间通过 "[v1,v2,...]" 字面量 + {@code ::vector} cast 转换。
 *
 * <p>{@link #findSimilar} 实现「候选集内语义 Top-K」（方案 §6.2）：限定在边界框预过滤得到的
 * poi_id 集合内做余弦近邻，而非全局相似检索 —— 这是与项目一通用 RAG 的核心差异化。
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class PoiEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * upsert 单个 POI 向量（poi_id 冲突则更新 content/embedding/updated_at）。
     */
    public void upsert(Long poiId, String content, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO poi_embedding (poi_id, content, embedding, updated_at) " +
                "VALUES (?, ?, ?::vector, now()) " +
                "ON CONFLICT (poi_id) DO UPDATE SET " +
                "  content = EXCLUDED.content, embedding = EXCLUDED.embedding, updated_at = now()",
                poiId, content, toVectorLiteral(embedding));
    }

    /**
     * 候选集内语义 Top-K：score = 1 - 余弦距离（越大越相似）。候选为空则返回空列表。
     */
    public List<PoiScore> findSimilar(List<Long> candidatePoiIds, float[] queryEmbedding, int topK) {
        if (candidatePoiIds == null || candidatePoiIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", candidatePoiIds.stream().map(id -> "?").toList());
        String sql = "SELECT poi_id, content, 1 - (embedding <=> ?::vector) AS score " +
                     "FROM poi_embedding " +
                     "WHERE poi_id IN (" + placeholders + ") " +
                     "ORDER BY embedding <=> ?::vector " +
                     "LIMIT ?";
        List<Object> params = new ArrayList<>();
        params.add(toVectorLiteral(queryEmbedding));
        params.addAll(candidatePoiIds);
        params.add(toVectorLiteral(queryEmbedding));
        params.add(topK);
        return jdbcTemplate.query(sql,
                (rs, i) -> new PoiScore(rs.getLong("poi_id"), rs.getString("content"), rs.getDouble("score")),
                params.toArray());
    }

    public int countAll() {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM poi_embedding", Integer.class);
        return n == null ? 0 : n;
    }

    public void deleteByPoiId(Long poiId) {
        jdbcTemplate.update("DELETE FROM poi_embedding WHERE poi_id = ?", poiId);
    }

    /** float[] → pgvector 文本字面量 "[0.1,0.2,...]"。 */
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

    /** 检索结果：poi_id / 原始语料 / 余弦相似度得分。 */
    public record PoiScore(Long poiId, String content, double score) {}
}
