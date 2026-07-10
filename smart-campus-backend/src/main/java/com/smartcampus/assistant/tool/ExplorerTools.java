package com.smartcampus.assistant.tool;

import com.smartcampus.assistant.dto.response.RetrievalResult;
import com.smartcampus.assistant.service.AssistantRetrievalService;
import com.smartcampus.assistant.service.AssistantUserPreferenceService;
import com.smartcampus.dto.response.RoutePlanResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.enums.RouteMode;
import com.smartcampus.service.POIService;
import com.smartcampus.service.RouteService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 探索助手的工具集（M3 + M7，方案 §7；getUserPreferences 为升级方案 P2 新增）。
 *
 * <p>用 Spring AI {@code @Tool} 把平台已有能力暴露给 ChatClient（Function Calling / Tool Use），
 * 全部<b>复用现有 Service/Repository</b>，不新建业务逻辑：
 * <ul>
 *   <li>{@link #recommendNearbyPlaces} —— 核心：空间约束 + 语义检索（复用 {@link AssistantRetrievalService}）。</li>
 *   <li>{@link #searchPlaces} —— 按名称/类别查地点（复用 {@link POIService}）。</li>
 *   <li>{@link #planRoute} —— 路线规划（复用 {@link RouteService}，高德）。</li>
 *   <li>{@link #listCategories} —— 列出可用地点类别，帮助模型澄清意图。</li>
 *   <li>{@link #getUserPreferences} —— 汇总当前用户收藏/签到的类别偏好（复用
 *       {@link AssistantUserPreferenceService}），userId 经 {@link ToolContext} 由服务端注入，
 *       不作为可由模型填写的参数（防止越权读取他人数据）。</li>
 * </ul>
 *
 * <p>每个工具捕获异常并返回<b>自然语言错误串</b>（不抛出），让模型能优雅兜底而非中断对话；
 * 同时向 {@link MeterRegistry} 上报失败计数（M7/P4），便于观测高德路线 API、POI 检索等
 * 依赖的隐性故障率。仅当 {@code app.assistant.enabled=true} 时装配。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class ExplorerTools {

    private static final int MAX_LIST = 10;
    /** 单次工具返回文本的硬上限（字符）：MAX_LIST 已限制条数，这里是防御性兜底，
     *  防止未来放宽 MAX_LIST 或某条 description 异常长时把 prompt 撑得过大。 */
    private static final int MAX_RESULT_CHARS = 4000;

    private final AssistantRetrievalService retrievalService;
    private final POIService poiService;
    private final RouteService routeService;
    private final AssistantUserPreferenceService userPreferenceService;
    private final MeterRegistry meterRegistry;

    @Tool(description = "根据用户当前位置和自然语言需求，推荐附近的地点。先按半径做地理围栏，再在范围内做语义相关排序。需要地点推荐时优先用本工具。")
    public String recommendNearbyPlaces(
            @ToolParam(description = "用户的需求描述，如'适合周末带娃的地方''安静的咖啡馆'") String query,
            @ToolParam(description = "用户当前纬度") double lat,
            @ToolParam(description = "用户当前经度") double lng,
            @ToolParam(description = "搜索半径（米），不确定可传 null 用默认值", required = false) Integer radiusMeters) {
        try {
            RetrievalResult result = retrievalService.search(query, lat, lng, radiusMeters);
            if (result.results().isEmpty()) {
                return "在该位置附近半径范围内没有找到与「" + query + "」相关的地点。可以建议用户扩大范围或更换关键词。";
            }
            StringBuilder sb = new StringBuilder("附近相关地点（共 " + result.candidateCount() + " 个候选，返回最相关的 "
                    + result.results().size() + " 个）：\n");
            int i = 1;
            for (RetrievalResult.PoiHit h : result.results()) {
                sb.append(i++).append(". ").append(h.name())
                        .append("（").append(h.category()).append("）")
                        .append(" — 距你约 ").append(Math.round(h.distanceMeters())).append(" 米")
                        .append("，相关度 ").append(String.format("%.2f", h.score()));
                if (h.description() != null && !h.description().isBlank()) {
                    sb.append("；简介：").append(truncate(h.description(), 60));
                }
                sb.append("\n");
            }
            return capResult(sb.toString());
        } catch (Exception e) {
            log.warn("recommendNearbyPlaces 失败：{}", e.getMessage());
            recordToolError("recommendNearbyPlaces");
            return "推荐地点时出错了，请稍后再试。";
        }
    }

    @Tool(description = "按名称关键词和/或类别精确查找平台内的地点。用于用户明确指名某地或某类别（如'图书馆''公园'）时。")
    public String searchPlaces(
            @ToolParam(description = "名称关键词，可为 null", required = false) String name,
            @ToolParam(description = "地点类别，可为 null", required = false) String category) {
        try {
            List<POI> pois;
            boolean hasName = name != null && !name.isBlank();
            boolean hasCategory = category != null && !category.isBlank();
            if (hasName && hasCategory) {
                pois = poiService.searchByNameAndCategory(name, category);
            } else if (hasName) {
                pois = poiService.searchByName(name);
            } else if (hasCategory) {
                pois = poiService.searchByCategory(category);
            } else {
                return "请提供名称关键词或类别中的至少一个。可用类别可调用 listCategories 工具查询。";
            }
            if (pois.isEmpty()) {
                return "没有找到符合条件的地点。";
            }
            StringBuilder sb = new StringBuilder("找到 " + pois.size() + " 个地点：\n");
            int i = 1;
            for (POI p : pois.stream().limit(MAX_LIST).toList()) {
                sb.append(i++).append(". ").append(p.getName())
                        .append("（").append(p.getCategory()).append("）");
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append(" — ").append(truncate(p.getDescription(), 60));
                }
                sb.append("\n");
            }
            if (pois.size() > MAX_LIST) {
                sb.append("……仅显示前 ").append(MAX_LIST).append(" 个。\n");
            }
            return capResult(sb.toString());
        } catch (Exception e) {
            log.warn("searchPlaces 失败：{}", e.getMessage());
            recordToolError("searchPlaces");
            return "查找地点时出错了，请稍后再试。";
        }
    }

    @Tool(description = "规划两点之间的路线，返回距离与预计时长。当用户问'怎么走/路线/多远/多久到'时使用。")
    public String planRoute(
            @ToolParam(description = "起点纬度") double startLat,
            @ToolParam(description = "起点经度") double startLng,
            @ToolParam(description = "终点纬度") double endLat,
            @ToolParam(description = "终点经度") double endLng,
            @ToolParam(description = "出行方式：walking 步行 / driving 驾车 / bicycling 骑行，默认 walking", required = false) String mode) {
        try {
            RouteMode routeMode;
            try {
                routeMode = RouteMode.fromValue(mode);
            } catch (IllegalArgumentException ex) {
                return "不支持的出行方式：" + mode + "（仅支持 步行/驾车/骑行）。";
            }
            RoutePlanResponse r = routeService.planRoute(startLat, startLng, endLat, endLng, routeMode);
            return String.format("%s路线：全程约 %s，预计 %s。",
                    r.getModeLabel(),
                    r.getDistanceText() != null ? r.getDistanceText() : (r.getDistanceMeters() + " 米"),
                    r.getDurationText() != null ? r.getDurationText() : (r.getDurationSeconds() + " 秒"));
        } catch (Exception e) {
            log.warn("planRoute 失败：{}", e.getMessage());
            recordToolError("planRoute");
            return "规划路线时出错了，请确认起终点坐标是否正确。";
        }
    }

    @Tool(description = "列出平台内所有可用的地点类别，帮助理解用户意图或在用户表述模糊时给出选项。")
    public String listCategories() {
        try {
            List<String> categories = poiService.getAllCategories();
            if (categories == null || categories.isEmpty()) {
                return "暂无可用类别。";
            }
            return "可用地点类别：" + String.join("、", categories);
        } catch (Exception e) {
            log.warn("listCategories 失败：{}", e.getMessage());
            recordToolError("listCategories");
            return "获取类别时出错了。";
        }
    }

    @Tool(description = "了解当前用户历史上常收藏/常打卡的地点类别偏好，仅在用户需求宽泛、未说明具体偏好时调用，用于让推荐更贴合其习惯。")
    public String getUserPreferences(ToolContext toolContext) {
        try {
            Long userId = extractUserId(toolContext);
            if (userId == null) {
                return "无法获取用户身份信息，本次按通用偏好推荐即可。";
            }
            Map<String, Long> favoriteCounts = userPreferenceService.summarizeFavoriteCategories(userId);
            List<String> checkedInCategories = userPreferenceService.checkedInCategories(userId);
            if (favoriteCounts.isEmpty() && checkedInCategories.isEmpty()) {
                return "该用户暂无收藏/签到记录，没有可参考的偏好数据，按通用推荐即可。";
            }
            StringBuilder sb = new StringBuilder("该用户的历史偏好（仅供参考，不代表本次一定要推荐同类）：\n");
            if (!favoriteCounts.isEmpty()) {
                sb.append("常收藏类别：");
                sb.append(String.join("、", favoriteCounts.entrySet().stream()
                        .map(e -> e.getKey() + " x" + e.getValue())
                        .toList()));
                sb.append("\n");
            }
            if (!checkedInCategories.isEmpty()) {
                sb.append("签到过的类别：").append(String.join("、", checkedInCategories)).append("\n");
            }
            return capResult(sb.toString());
        } catch (Exception e) {
            log.warn("getUserPreferences 失败：{}", e.getMessage());
            recordToolError("getUserPreferences");
            return "获取用户偏好时出错了，按通用推荐即可。";
        }
    }

    /**
     * 从 {@link ToolContext} 取出服务端注入的 userId（见 {@code AssistantChatService#toolContext}）。
     * ToolContext 不出现在暴露给模型的工具 JSON Schema 里，模型无法读取或伪造其内容，
     * 是把"当前操作者是谁"这类敏感上下文安全传给工具的标准做法。
     */
    private static Long extractUserId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object v = toolContext.getContext().get("userId");
        return v instanceof Long l ? l : null;
    }

    private void recordToolError(String toolName) {
        try {
            meterRegistry.counter("assistant.tool.errors", "tool", toolName).increment();
        } catch (Exception ignored) {
            // 指标上报失败不应影响工具本身的降级返回
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String capResult(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= MAX_RESULT_CHARS ? s : s.substring(0, MAX_RESULT_CHARS) + "\n……（内容过长，已截断）";
    }
}
