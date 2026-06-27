package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.assistant.tool.ExplorerTools;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AI 探索助手对话服务（M3 + M4，方案 §5/§7/§8）。
 *
 * <p>M3：用 Spring AI {@link ChatClient} 编排 Agent，挂载 {@link ExplorerTools}，流式返回。
 * <p>M4：接入<b>语义缓存</b>（命中即跳过 LLM/工具，直接回放缓存）与 <b>Micrometer 指标</b>
 * （请求数 / 缓存命中率 / 端到端延迟，经现有 Prometheus + Grafana 观测）。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantChatService {

    private static final String SYSTEM_PROMPT = """
            你是「地点探索」社区的 AI 探索助手，帮助用户发现身边的好地方。

            行为规则：
            1. 必须通过工具获取平台内的真实地点数据，绝不凭空编造地点；没有结果就如实告知。
               推荐的地点【数量与内容必须与工具返回结果完全一致】：工具返回几个就推荐几个，
               不得补充、虚构任何工具结果之外的地点（哪怕你"知道"某个地方）。
            2. 需要"附近推荐"时调用 recommendNearbyPlaces；用户指名某地/某类别时用 searchPlaces；
               问"怎么走/路线/多远/多久"时用 planRoute；意图模糊时可用 listCategories 给选项。
            3. 用户的当前位置会在消息中给出，调用需要坐标的工具时必须使用该位置，不要询问或臆造坐标。
            4. 推荐时给出：地点名称、类别、与用户的距离、简短推荐理由（基于地点简介）。
            5. 【距离必须照实】每个地点的距离要严格引用工具返回的对应数值，逐个如实写，
               不同地点的距离通常各不相同；严禁把多个地点的距离写成同一个值，也不得自行编造或四舍五入到失真。
            6. 只回答与地点探索/出行相关的问题，其它话题礼貌婉拒。
            7. 全程使用简体中文，不要夹杂英文单词（如 nearby）；简洁友好；不要输出工具调用的原始 JSON。
            """;

    private static final int CACHE_REPLAY_CHUNK = 30;

    private final ChatClient chatClient;
    private final ExplorerTools explorerTools;
    private final AssistantProperties properties;
    private final AssistantSemanticCache semanticCache;
    private final MeterRegistry meterRegistry;

    public AssistantChatService(ChatClient chatClient,
                                ExplorerTools explorerTools,
                                AssistantProperties properties,
                                AssistantSemanticCache semanticCache,
                                MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.explorerTools = explorerTools;
        this.properties = properties;
        this.semanticCache = semanticCache;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 流式对话：先查语义缓存，命中则回放缓存；未命中走 Agent，并在完成时写回缓存。
     */
    public Flux<String> stream(String message, double lat, double lng, Integer radius) {
        meterRegistry.counter("assistant.chat.requests").increment();
        long startNanos = System.nanoTime();

        Optional<String> cached = semanticCache.lookup(message, lat, lng);
        if (cached.isPresent()) {
            meterRegistry.counter("assistant.chat.cache", "result", "hit").increment();
            recordLatency(startNanos);
            return Flux.fromIterable(splitForReplay(cached.get()));
        }
        meterRegistry.counter("assistant.chat.cache", "result", "miss").increment();

        // 说明：Spring AI 2.0「流式 .stream() + 工具调用」对 DashScope/Qwen 的工具增量解析存在兼容问题
        // （NoSuchElementException: No value present）。这里改用更成熟、兼容的非流式 call()（工具调用稳定），
        // 拿到完整回答后分块"流式回放"给前端——保留 Agent + Tool Use，前端 SSE 体验不变。
        // 放到 boundedElastic 线程执行阻塞调用，避免占用请求/事件循环线程。
        return Flux.defer(() -> {
                    try {
                        String reply = chatClient.prompt()
                                .system(SYSTEM_PROMPT)
                                .user(buildUserContent(message, lat, lng, radius))
                                .tools(explorerTools)
                                .call()
                                .content();
                        semanticCache.put(message, lat, lng, reply);
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

    /** 同步对话（非流式），用于测试/降级。 */
    public String chat(String message, double lat, double lng, Integer radius) {
        Optional<String> cached = semanticCache.lookup(message, lat, lng);
        if (cached.isPresent()) {
            return cached.get();
        }
        String reply = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserContent(message, lat, lng, radius))
                .tools(explorerTools)
                .call()
                .content();
        semanticCache.put(message, lat, lng, reply);
        return reply;
    }

    private String buildUserContent(String message, double lat, double lng, Integer radius) {
        int effectiveRadius = radius != null ? radius : properties.getRetrieval().getRadiusMeters();
        return """
                用户当前位置：纬度 %f，经度 %f，建议搜索半径 %d 米。
                用户消息：%s
                """.formatted(lat, lng, effectiveRadius, message);
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
