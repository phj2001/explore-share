package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIFavoriteResponse;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.service.POIFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 探索助手 · 用户偏好聚合（M7，升级方案 P2）。
 *
 * <p>只读聚合用户的收藏（{@link POIFavoriteService}，服务层已有分页接口，直接复用）与
 * 签到类别分布（{@link POICheckInRepository#findDistinctCategoriesByUserId}，仓储层已有现成
 * 聚合查询，同 {@link AssistantRetrievalService} 直连仓储的做法保持一致），供
 * {@link com.smartcampus.assistant.tool.ExplorerTools#getUserPreferences} 工具调用。
 *
 * <p>只返回类别聚合结果（如"咖啡馆 x3、图书馆 x1"），不返回具体地点名称/时间等细节——
 * 足够模型据此调整推荐口味，同时不把用户的完整行踪细节喂给 LLM。
 *
 * <p><b>容错（fail-open）</b>：查询异常记日志、返回空结果，不影响主对话（退化为无个性化）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantUserPreferenceService {

    private final POIFavoriteService favoriteService;
    private final POICheckInRepository checkInRepository;
    private final AssistantProperties properties;

    /**
     * 汇总用户常收藏 / 常签到的地点类别，按出现次数降序（收藏），签到类别无计数只给"是否去过"。
     * 两个来源都为空时返回空 Map（调用方据此提示"暂无偏好数据"）。
     */
    public Map<String, Long> summarizeFavoriteCategories(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        try {
            int maxRecords = properties.getPersonalization().getMaxRecords();
            PageResponse<POIFavoriteResponse> favorites = favoriteService.getUserFavorites(userId, 1, maxRecords);
            if (favorites == null || favorites.getRecords() == null || favorites.getRecords().isEmpty()) {
                return Map.of();
            }
            Map<String, Long> counts = new LinkedHashMap<>();
            for (POIFavoriteResponse f : favorites.getRecords()) {
                String category = f.getCategory();
                if (category != null && !category.isBlank()) {
                    counts.merge(category, 1L, Long::sum);
                }
            }
            return counts;
        } catch (Exception e) {
            log.warn("读取用户收藏偏好失败（忽略）：{}", e.getMessage());
            return Map.of();
        }
    }

    /** 用户签到过的地点类别（去重，不含次数）。 */
    public List<String> checkedInCategories(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            return checkInRepository.findDistinctCategoriesByUserId(userId);
        } catch (Exception e) {
            log.warn("读取用户签到偏好失败（忽略）：{}", e.getMessage());
            return List.of();
        }
    }
}
