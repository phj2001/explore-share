package com.smartcampus.assistant.dto.response;

import java.util.List;

/**
 * 空间约束混合检索结果（M2）。
 *
 * <ul>
 *   <li>{@code candidateCount}：半径圆内候选 POI 总数（边界框 + Haversine 精确过滤后）。</li>
 *   <li>{@code results}：候选集内语义 Top-K，已按语义相关度降序（方案 §6.2：先地理围栏、再语义排序）。</li>
 * </ul>
 *
 * <p>圆内无候选时 {@code results} 为空且不视为错误（M2 验收要点）。
 */
public record RetrievalResult(
        String query,
        int candidateCount,
        List<PoiHit> results
) {

    /** 单条命中：POI 主体信息 + 与中心的精确距离 + 语义相似度得分。 */
    public record PoiHit(
            Long poiId,
            String name,
            String category,
            String description,
            double latitude,
            double longitude,
            /** 与中心点的 Haversine 精确距离（米）。 */
            double distanceMeters,
            /** 语义相似度（1 - 余弦距离，越大越相关）。 */
            double score
    ) {
    }
}
