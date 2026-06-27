package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.assistant.dto.response.RetrievalResult;
import com.smartcampus.assistant.dto.response.RetrievalResult.PoiHit;
import com.smartcampus.assistant.repository.PoiEmbeddingRepository;
import com.smartcampus.assistant.repository.PoiEmbeddingRepository.PoiScore;
import com.smartcampus.entity.POI;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 空间约束混合检索（M2 差异化核心，方案 §6.2）。
 *
 * <p>三段式「先地理围栏、再语义排序」——地图域独有的 RAG，区别于项目一通用语义检索：
 * <ol>
 *   <li>边界框预过滤：{@link GeoUtils#calculateBounds}（半径单位为<b>千米</b>，米需 /1000）+
 *       {@link POIRepository#findWithinBounds}，复用既有经纬度 BETWEEN 查询，非 ST_DWithin（任务规则 4）。</li>
 *   <li>Haversine 精确过滤：边界框是半径圆的外接正方形，剔除四角超出真实半径的点。</li>
 *   <li>候选集内语义 Top-K：{@link PoiEmbeddingRepository#findSimilar} 限定在圆内 id 集合做余弦近邻。</li>
 * </ol>
 *
 * <p>圆内无候选 → 返回空 results（非错误）。embed 调用失败向上抛（M4 护栏层统一兜底）。
 * M3 Agent 的 searchPois/nearbyPois 工具直接复用本服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantRetrievalService {

    private final OpenAiEmbeddingModel embeddingModel;
    private final PoiEmbeddingRepository poiEmbeddingRepository;
    private final POIRepository poiRepository;
    private final AssistantProperties properties;

    /**
     * 空间约束混合检索。
     *
     * @param query          自然语言问题
     * @param lat            中心纬度
     * @param lng            中心经度
     * @param radiusOverride 可选半径（米），null 取配置默认值
     * @return 圆内候选总数 + 语义 Top-K 命中（已按相关度降序）
     */
    public RetrievalResult search(String query, double lat, double lng, Integer radiusOverride) {
        int radiusMeters = radiusOverride != null ? radiusOverride : properties.getRetrieval().getRadiusMeters();
        int topK = properties.getRetrieval().getTopK();

        // 1. 边界框预过滤（calculateBounds 半径单位为千米 → /1000）
        double[] bounds = GeoUtils.calculateBounds(lat, lng, radiusMeters / 1000.0);
        List<POI> inBox = poiRepository.findWithinBounds(bounds[0], bounds[1], bounds[2], bounds[3]);

        // 2. Haversine 精确过滤：框内但圆外的点剔除，同时记录精确距离供结果回传
        List<Candidate> within = inBox.stream()
                .map(p -> new Candidate(p, GeoUtils.haversineMeters(
                        lat, lng, p.getLatitude().doubleValue(), p.getLongitude().doubleValue())))
                .filter(c -> c.distance <= radiusMeters)
                .toList();

        if (within.isEmpty()) {
            log.info("检索无候选：query=\"{}\", ({},{}) 半径={}m，边界框内共 {} 个 POI", query, lat, lng, radiusMeters, inBox.size());
            return new RetrievalResult(query, 0, List.of());
        }

        // 3. 问题嵌入（必须与 POI 入库同模型，向量空间才一致）
        float[] queryVector = embeddingModel.embed(query);

        // 4. 候选集内语义 Top-K（已按 embedding<=>query 升序，即相关度降序）
        List<Long> candidateIds = within.stream().map(c -> c.poi.getId()).toList();
        List<PoiScore> scored = poiEmbeddingRepository.findSimilar(candidateIds, queryVector, topK);

        // 5. 组装结果：findSimilar 返回的 poiId 回查候选 POI + 复用已算距离
        Map<Long, Candidate> byId = within.stream()
                .collect(Collectors.toMap(c -> c.poi.getId(), Function.identity(), (a, b) -> a));
        List<PoiHit> hits = scored.stream()
                .map(s -> {
                    Candidate c = byId.get(s.poiId());
                    if (c == null) {
                        return null; // 理论不发生：findSimilar 已限定在 candidateIds 内
                    }
                    POI p = c.poi;
                    return new PoiHit(p.getId(), p.getName(), p.getCategory(), p.getDescription(),
                            p.getLatitude().doubleValue(), p.getLongitude().doubleValue(),
                            c.distance, s.score());
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("检索完成：query=\"{}\", 圆内候选={}, 返回 Top-{}", query, within.size(), hits.size());
        return new RetrievalResult(query, within.size(), hits);
    }

    /** 候选中间结构：POI + 与中心的精确 Haversine 距离（米）。 */
    private record Candidate(POI poi, double distance) {
    }
}
