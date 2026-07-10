package com.smartcampus.assistant.service;

import com.smartcampus.assistant.repository.PoiEmbeddingRepository;
import com.smartcampus.entity.POI;
import com.smartcampus.repository.POIRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * POI 嵌入管道（M1）。
 *
 * <p>语料 = name + category + description（方案 §6.1，以 POI 主体文本为主）。
 * 嵌入由 Spring AI {@link OpenAiEmbeddingModel}（DashScope text-embedding-v4，1024 维）生成，
 * 经 {@link PoiEmbeddingRepository#upsert} 入 poi_embedding。
 *
 * <p>全量 {@link #embedAll()}：遍历所有 POI 逐条嵌入 upsert（M1 先逐条，批量优化留后续）。
 * 增量 {@link #refresh(Long)}：POI 新增/更新后刷新对应向量（M3 在 POIService 写路径埋点，或定时增量）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;
    private final PoiEmbeddingRepository poiEmbeddingRepository;
    private final POIRepository poiRepository;

    /**
     * 全量嵌入入库，返回统计（不抛异常：单条失败计入 fail，不影响其余）。
     */
    public EmbedStats embedAll() {
        List<POI> pois = poiRepository.findAll();
        final int batchSize = 9; // 保守批量（远低于 DashScope embedding 批量上限），规避 QPS 限制
        int ok = 0;
        int fail = 0;
        for (int start = 0; start < pois.size(); start += batchSize) {
            List<POI> batch = pois.subList(start, Math.min(start + batchSize, pois.size()));
            List<String> contents = batch.stream().map(this::buildContent).toList();
            try {
                // 批量嵌入：一次 API 调用处理一批（结果顺序与输入一致），大幅减少往返、规避 QPS 限流
                List<float[]> vectors = embeddingModel.embed(contents);
                for (int j = 0; j < batch.size(); j++) {
                    poiEmbeddingRepository.upsert(batch.get(j).getId(), contents.get(j), vectors.get(j));
                    ok++;
                }
            } catch (Exception e) {
                fail += batch.size();
                log.error("批量嵌入失败 [{}-{}]: {}", start, start + batch.size(), e.getMessage());
            }
        }
        log.info("全量嵌入完成：总数={}, 成功={}, 失败={}", pois.size(), ok, fail);
        return new EmbedStats(pois.size(), ok, fail);
    }


    /**
     * 单点刷新：拼文本 → embed → upsert。
     */
    public void refresh(POI poi) {
        String content = buildContent(poi);
        float[] vector = embeddingModel.embed(content);
        poiEmbeddingRepository.upsert(poi.getId(), content, vector);
    }

    /**
     * 按 poiId 刷新（增量入口）。
     */
    public void refresh(Long poiId) {
        POI poi = poiRepository.findById(poiId)
                .orElseThrow(() -> new IllegalArgumentException("POI 不存在: " + poiId));
        refresh(poi);
    }

    /** 语料拼装：name / category / description，以全角分隔符连接。 */
    private String buildContent(POI poi) {
        StringBuilder sb = new StringBuilder();
        if (poi.getName() != null) {
            sb.append(poi.getName());
        }
        if (poi.getCategory() != null) {
            sb.append("｜").append(poi.getCategory());
        }
        if (poi.getDescription() != null && !poi.getDescription().isBlank()) {
            sb.append("｜").append(poi.getDescription());
        }
        return sb.toString();
    }

    /** 嵌入统计：总数 / 成功 / 失败。 */
    public record EmbedStats(int total, int success, int fail) {}
}
