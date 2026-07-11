package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.assistant.tool.ExplorerTools;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * AI 探索助手对话服务（M3 + M4 + M7，方案 §5/§7/§8，M7 为升级方案 P1/P2）。
 *
 * <p>M3：用 Spring AI {@link ChatClient} 编排 Agent，挂载 {@link ExplorerTools}，流式返回。
 * <p>M4：接入<b>语义缓存</b>（命中即跳过 LLM/工具，直接回放缓存）与 <b>Micrometer 指标</b>
 * （请求数 / 缓存命中率 / 端到端延迟，经现有 Prometheus + Grafana 观测）。
 * <p>M7：接入 {@link AssistantConversationMemory} 支持多轮对话；有历史的多轮请求跳过语义缓存
 * （同一句话在不同上下文语义不同，缓存会导致答非所问）；经 {@code toolContext} 把 userId
 * 安全透传给工具（不进模型可见文本，杜绝越权读取他人数据），供 {@link ExplorerTools#getUserPreferences}
 * 做个性化推荐。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantChatService {

    private static final String SYSTEM_PROMPT = """
            你是「地点探索」社区的 AI 探索助手。你有两重身份：
            ① 地点探索专家（核心专长）：帮助用户发现身边的好地方、规划路线；
            ② 通用助手：地点之外的日常问题（闲聊、常识、写作、翻译、计算、时间等）也正常回答，
               像一个友好的通用聊天助手一样自然应对，不要以"我只负责地点推荐"为由拒绝。

            地点探索规则（仅在涉及地点推荐/查询/路线时适用）：
            1. 必须通过工具获取平台内的真实地点数据，绝不凭空编造地点；没有结果就如实告知。
               推荐的地点【数量与内容必须与工具返回结果完全一致】：工具返回几个就推荐几个，
               不得补充、虚构任何工具结果之外的地点（哪怕你"知道"某个地方）。
            2. 需要"附近推荐"时调用 recommendNearbyPlaces；用户指名某地/某类别时用 searchPlaces；
               问"怎么走/路线/多远/多久"时用 planRoute；意图模糊时可用 listCategories 给选项。
            3. 用户的当前位置会在消息中给出，调用需要坐标的工具时必须使用该位置，不要询问或臆造坐标。
            4. 推荐时给出：地点名称、类别、与用户的距离、简短推荐理由（基于地点简介）。
            5. 【距离必须照实】每个地点的距离要严格引用工具返回的对应数值，逐个如实写，
               不同地点的距离通常各不相同；严禁把多个地点的距离写成同一个值，也不得自行编造或四舍五入到失真。

            通用问答规则：
            6. 回答一般问题时不要调用地点工具；用你自身的知识直接回答，简洁准确。
            7. 消息里携带的「当前时间」「用户位置（经纬度）」是后台上下文，按需使用：仅当用户明确询问时间/日期时才报时间、需要附近推荐/路线时才用位置；【严禁】在用户未询问时主动报出时间、日期、坐标，或把坐标反推成城市名告诉用户。打招呼、闲聊、回答通用知识时不要提及这些上下文。
            8. 无法确定的时效性信息（如实时新闻、天气、股价）要坦诚说明无法获取，不要编造。

            多轮对话规则：
            9. 如果本次消息之前有历史对话，且用户消息中出现"这个""那个""刚才""再来一个""远一点/近一点"
               等指代或追问表达，结合历史消息理解真实意图，不要孤立地按字面处理当前这一句。

            个性化规则：
            10. 仅当用户需求比较宽泛、没有说明具体偏好时（如"随便推荐个地方""附近有什么好玩的"），
                可先调用 getUserPreferences 了解其历史收藏/签到类别偏好，让推荐更贴合其习惯；
                用户已经明确说了想要什么类型（如"找家咖啡馆"）时，不必调用，直接按其明确需求处理。

            11. 全程使用简体中文，不要夹杂英文单词（如 nearby）；简洁友好；不要输出工具调用的原始 JSON。
            """;

    private static final int CACHE_REPLAY_CHUNK = 30;

    /** 时效性问题特征（时间/日期/天气/新闻/实时行情），命中则绕过语义缓存。 */
    private static final Pattern TIME_SENSITIVE_PATTERN = Pattern.compile(
            "几点|几号|时间|日期|今天|明天|昨天|现在|当前|星期|礼拜|周[一二三四五六日天末]|天气|气温|温度|新闻|股价|汇率|比分");

    private final ChatClient chatClient;
    private final ExplorerTools explorerTools;
    private final AssistantProperties properties;
    private final AssistantSemanticCache semanticCache;
    private final AssistantConversationMemory conversationMemory;
    private final MeterRegistry meterRegistry;

    public AssistantChatService(ChatClient chatClient,
                                ExplorerTools explorerTools,
                                AssistantProperties properties,
                                AssistantSemanticCache semanticCache,
                                AssistantConversationMemory conversationMemory,
                                MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.explorerTools = explorerTools;
        this.properties = properties;
        this.semanticCache = semanticCache;
        this.conversationMemory = conversationMemory;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 流式对话：先查语义缓存，命中则回放缓存；未命中走 Agent，并在完成时写回缓存。
     *
     * @param conversationId 会话 ID（M7/P1），为空则视为无历史的单轮请求，行为与升级前一致
     * @param userId         当前登录用户 ID（M7/P2），经 toolContext 透传给工具做个性化查询；
     *                       未登录（理论上不会发生，controller 层已鉴权）时为 null
     */
    public Flux<String> stream(String message, double lat, double lng, Integer radius,
                                String conversationId, Long userId) {
        meterRegistry.counter("assistant.chat.requests").increment();
        long startNanos = System.nanoTime();

        List<Message> history = conversationMemory.loadHistory(conversationId);
        // 有历史的多轮请求跳过语义缓存：同一句话在不同上下文里含义可能完全不同（"这个""远一点"），
        // 缓存按单条 query 语义匹配，多轮场景下会答非所问；只对无上下文的首轮请求走缓存。
        boolean cacheable = history.isEmpty() && isCacheable(message);
        if (cacheable) {
            Optional<String> cached = semanticCache.lookup(message, lat, lng);
            if (cached.isPresent()) {
                meterRegistry.counter("assistant.chat.cache", "result", "hit").increment();
                recordLatency(startNanos);
                conversationMemory.append(conversationId, "user", message);
                conversationMemory.append(conversationId, "assistant", cached.get());
                return Flux.fromIterable(splitForReplay(cached.get()));
            }
        }
        meterRegistry.counter("assistant.chat.cache", "result", cacheable ? "miss" : "bypass").increment();

        // 说明：Spring AI 2.0「流式 .stream() + 工具调用」对 DashScope/Qwen 的工具增量解析存在兼容问题
        // （NoSuchElementException: No value present）。这里改用更成熟、兼容的非流式 call()（工具调用稳定），
        // 拿到完整回答后分块"流式回放"给前端——保留 Agent + Tool Use，前端 SSE 体验不变。
        // 放到 boundedElastic 线程执行阻塞调用，避免占用请求/事件循环线程。
        return Flux.defer(() -> {
                    try {
                        String reply = chatClient.prompt()
                                .system(SYSTEM_PROMPT)
                                .messages(history)
                                .user(buildUserContent(message, lat, lng, radius))
                                .tools(explorerTools)
                                .toolContext(toolContext(userId))
                                .call()
                                .content();
                        if (cacheable) {
                            semanticCache.put(message, lat, lng, reply);
                        }
                        conversationMemory.append(conversationId, "user", message);
                        conversationMemory.append(conversationId, "assistant", reply);
                        recordLatency(startNanos);
                        return Flux.fromIterable(splitForReplay(reply));
                    } catch (Exception e) {
                        log.warn("AI 对话生成异常：{}", e.getMessage());
                        recordLatency(startNanos);
                        return Flux.<String>error(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 经 ChatClient 的 toolContext 把 userId 传给工具（{@link ExplorerTools#getUserPreferences}）。
     * <b>不放进 system/user 文本</b>：ToolContext 只在 Java 侧对工具方法可见，模型看不到也改不了，
     * 避免用户在消息里声称"我是 userId=1"来越权读取别人的收藏/签到数据。
     */
    private Map<String, Object> toolContext(Long userId) {
        Map<String, Object> ctx = new HashMap<>();
        if (userId != null) {
            ctx.put("userId", userId);
        }
        return ctx;
    }

    /** 同步对话（非流式），用于测试/降级。 */
    public String chat(String message, double lat, double lng, Integer radius) {
        boolean cacheable = isCacheable(message);
        if (cacheable) {
            Optional<String> cached = semanticCache.lookup(message, lat, lng);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        String reply = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserContent(message, lat, lng, radius))
                .tools(explorerTools)
                .call()
                .content();
        if (cacheable) {
            semanticCache.put(message, lat, lng, reply);
        }
        return reply;
    }

    /**
     * 时效性问题（时间/日期/天气/新闻等）的回答会随时间变化，进入语义缓存会导致
     * "现在几点 → 缓存 24 小时重放旧时间"这类严重错误，必须绕过缓存（不查也不写）。
     * 启发式关键词匹配：宁可漏缓存（多花一次 LLM 调用），不可错缓存（答错基本事实）。
     */
    private static boolean isCacheable(String message) {
        return message == null || !TIME_SENSITIVE_PATTERN.matcher(message).find();
    }

    private String buildUserContent(String message, double lat, double lng, Integer radius) {
        int effectiveRadius = radius != null ? radius : properties.getRetrieval().getRadiusMeters();
        // 注入服务器当前时间：模型自身不知道"现在"，不注入则时间类问题必然编造或拒答
        LocalDateTime now = LocalDateTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "（" + now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA) + "）";
        return """
                当前时间：%s。
                用户当前位置：纬度 %f，经度 %f，建议搜索半径 %d 米。
                用户消息：%s
                """.formatted(currentTime, lat, lng, effectiveRadius, message);
    }

    private void recordLatency(long startNanos) {
        meterRegistry.timer("assistant.chat.latency").record(Duration.ofNanos(System.nanoTime() - startNanos));
    }

    /** 把缓存回答切成小块，模拟流式回放，保持前端体验一致。 */
    private static List<String> splitForReplay(String text) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += CACHE_REPLAY_CHUNK) {
            chunks.add(text.substring(i, Math.min(text.length(), i + CACHE_REPLAY_CHUNK)));
        }
        if (chunks.isEmpty()) {
            chunks.add("");
        }
        return chunks;
    }
}
