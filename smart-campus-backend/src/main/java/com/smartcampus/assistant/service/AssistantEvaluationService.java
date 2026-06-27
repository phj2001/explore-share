package com.smartcampus.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.assistant.dto.response.EvaluationReport;
import com.smartcampus.assistant.dto.response.EvaluationReport.CaseResult;
import com.smartcampus.assistant.dto.response.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 探索助手评测服务（M6，方案 §8）。
 *
 * <p>用「固定用例集 + LLM-as-judge」量化推荐质量，呼应项目一的评测方法论，产出可写进简历的基线分：
 * <ul>
 *   <li><b>relevance 相关性</b>：回答是否切题、给出合适地点。</li>
 *   <li><b>groundedness 依据性</b>：回答中推荐的地点是否都来自检索候选（防编造、隐含"在地理约束内"）。</li>
 * </ul>
 *
 * <p>由管理员端点触发，在给定 (lat,lng)（需该处有 POI 数据）跑全部用例。
 * <b>建议评测时关闭语义缓存</b>（ASSISTANT_CACHE_ENABLED=false），避免命中缓存导致每例非独立生成。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantEvaluationService {

    /** 固定评测用例（覆盖推荐 / 类别 / 路线 / 模糊意图等典型表述）。 */
    private static final List<String> EVAL_QUERIES = List.of(
            "推荐附近适合周末带娃的地方",
            "附近有什么安静的咖啡馆",
            "想找个适合拍照打卡的地点",
            "附近的图书馆或自习的地方",
            "推荐几个适合朋友聚餐的餐厅",
            "附近有公园或者能散步的绿地吗",
            "想看看附近的文化景点",
            "附近适合情侣约会的地方",
            "有没有适合运动健身的场所",
            "推荐几个购物的好去处",
            "附近评价比较好的地方有哪些",
            "想找个安静看书的角落"
    );

    private static final String JUDGE_SYSTEM = """
            你是地点推荐系统的质量评审。基于"用户问题 + 系统检索到的候选地点列表 + 助手回答"打分，仅输出 JSON：
            {"relevance": 0~1 的小数, "groundedness": 0~1 的小数, "analysis": "简短中文说明"}
            评分标准：
            - relevance：助手回答是否切题、是否给出与问题匹配的地点推荐。
            - groundedness：助手回答中提到的推荐地点是否都来自"候选地点列表"（出现列表外、疑似编造的地点要扣分；若回答如实说明无结果则视为高分）。
            只输出 JSON，不要任何额外文字或代码块标记。
            """;

    private final AssistantRetrievalService retrievalService;
    private final AssistantChatService chatService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AssistantEvaluationService(AssistantRetrievalService retrievalService,
                                      AssistantChatService chatService,
                                      ChatClient chatClient,
                                      ObjectMapper objectMapper) {
        this.retrievalService = retrievalService;
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public EvaluationReport evaluate(double lat, double lng) {
        List<CaseResult> details = new ArrayList<>();
        double sumRel = 0;
        double sumGround = 0;
        int scored = 0;

        for (String query : EVAL_QUERIES) {
            try {
                RetrievalResult retrieval = retrievalService.search(query, lat, lng, null);
                String candidateNames = retrieval.results().stream()
                        .map(RetrievalResult.PoiHit::name)
                        .collect(Collectors.joining("、"));
                String answer = chatService.chat(query, lat, lng, null);

                double[] scores = judge(query, candidateNames, answer);
                double relevance = scores[0];
                double groundedness = scores[1];
                sumRel += relevance;
                sumGround += groundedness;
                scored++;

                details.add(new CaseResult(query, retrieval.candidateCount(),
                        preview(answer), round(relevance), round(groundedness), null));
            } catch (Exception e) {
                log.warn("评测用例失败：{} → {}", query, e.getMessage());
                details.add(new CaseResult(query, 0, "", 0, 0, "评测失败: " + e.getMessage()));
            }
        }

        double avgRel = scored > 0 ? round(sumRel / scored) : 0;
        double avgGround = scored > 0 ? round(sumGround / scored) : 0;
        log.info("AI 助手评测完成：用例 {}，relevance={}，groundedness={}", EVAL_QUERIES.size(), avgRel, avgGround);
        return new EvaluationReport(EVAL_QUERIES.size(), avgRel, avgGround, details);
    }

    /** LLM-as-judge：返回 [relevance, groundedness]。解析失败返回 [0,0]。 */
    private double[] judge(String query, String candidateNames, String answer) {
        String userPrompt = """
                用户问题：%s

                系统检索到的候选地点列表：%s

                助手回答：%s
                """.formatted(query, candidateNames.isBlank() ? "（无候选）" : candidateNames, answer);
        try {
            String content = chatClient.prompt()
                    .system(JUDGE_SYSTEM)
                    .user(userPrompt)
                    .call()
                    .content();
            JsonNode node = objectMapper.readTree(stripFences(content));
            double rel = clamp01(node.path("relevance").asDouble(0));
            double ground = clamp01(node.path("groundedness").asDouble(0));
            return new double[]{rel, ground};
        } catch (Exception e) {
            log.warn("评审解析失败：{}", e.getMessage());
            return new double[]{0, 0};
        }
    }

    private static String stripFences(String s) {
        if (s == null) {
            return "{}";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        return t;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static String preview(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 80 ? s : s.substring(0, 80) + "…";
    }
}
