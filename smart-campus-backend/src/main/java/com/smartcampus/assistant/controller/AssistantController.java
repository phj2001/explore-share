package com.smartcampus.assistant.controller;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.assistant.dto.request.ChatRequest;
import com.smartcampus.assistant.dto.request.RetrievalRequest;
import com.smartcampus.assistant.dto.response.RetrievalResult;
import com.smartcampus.assistant.dto.response.EvaluationReport;
import com.smartcampus.assistant.service.AssistantChatService;
import com.smartcampus.assistant.service.AssistantEvaluationService;
import com.smartcampus.assistant.service.AssistantGuard;
import com.smartcampus.assistant.service.AssistantRateLimiter;
import com.smartcampus.assistant.service.AssistantRetrievalService;
import com.smartcampus.assistant.service.EmbeddingService;
import com.smartcampus.dto.common.Result;
import com.smartcampus.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;

/**
 * AI 探索助手接口（M1 嵌入入口 + M2 空间约束检索 + M3 对话）。
 *
 * <p>M0 的 {@code GET /dev/ping} 验证接口已随 M3 正式 {@code POST /chat} 上线移除
 * （遗留时任何登录用户可绕过护栏/限流直连 LLM，存在成本滥用风险）。
 *
 * <p>M1：{@code POST /admin/embed-all} 全量 POI 嵌入入库，限管理员，用于重建/刷新语义索引。
 *
 * <p>M2：{@code POST /retrieve} 空间约束混合检索，给定位置+问题返回"围栏内且语义相关"候选；
 * M3 Agent 的 searchPois/nearbyPois 工具将复用 {@link AssistantRetrievalService}。
 *
 * <p>鉴权：{@code /api/assistant/**} 登录态；admin 子路径额外要求 SUPER_ADMIN/ADMIN。
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantController {

    private final AssistantProperties properties;
    private final EmbeddingService embeddingService;
    private final AssistantRetrievalService retrievalService;
    private final AssistantChatService chatService;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantGuard guard;
    private final AssistantEvaluationService evaluationService;

    /**
     * 全量 POI 嵌入入库（M1）。限管理员：遍历所有 POI 生成向量 upsert，返回成功/失败统计。
     * 仅在 app.assistant.enabled=true 时本控制器装配。
     */
    @PostMapping("/admin/embed-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Result<EmbeddingService.EmbedStats> embedAll() {
        return Result.success(embeddingService.embedAll());
    }

    /**
     * AI 助手质量评测（M6）。限管理员。在给定 (lat,lng)（需该处有 POI 数据）跑固定用例集，
     * LLM-as-judge 给出 relevance / groundedness 基线分。建议评测时关闭语义缓存以保证每例独立生成。
     */
    @PostMapping("/admin/evaluate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Result<EvaluationReport> evaluate(@RequestParam double lat, @RequestParam double lng) {
        return Result.success(evaluationService.evaluate(lat, lng));
    }

    /**
     * 空间约束混合检索（M2 验收 + M3 Agent 复用）。
     * 在 (lat,lng) 半径范围内按 query 语义召回 Top-K POI，返回结构化候选。
     * 正式对话接口 {@code POST /chat}（SSE 流）在 M3 落地。
     */
    @PostMapping("/retrieve")
    public Result<RetrievalResult> retrieve(@Valid @RequestBody RetrievalRequest request,
                                            Authentication authentication) {
        // 与 /chat 共用每用户限流：每次检索都产生一次 embedding 计费调用，不能无限制开放
        Long userId = (authentication != null && authentication.getPrincipal() instanceof Long id) ? id : null;
        if (!rateLimiter.allow(userId)) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
        return Result.success(retrievalService.search(
                request.getQuery(), request.getLat(), request.getLng(), request.getRadius()));
    }

    /**
     * AI 探索助手对话（M3）：SSE 流式返回。Agent 自主调用工具做地点推荐 / 路线规划。
     * 前端用 EventSource / fetch-stream 消费；每个事件 data 为一段增量文本。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        // M4 生产级护栏（在请求线程执行，SecurityContext 可用；越权/超限在开流前拒绝）
        Long userId = (authentication != null && authentication.getPrincipal() instanceof Long id) ? id : null;
        if (!rateLimiter.allow(userId)) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
        String safeMessage = guard.check(request.getMessage()); // 注入/超长 → BusinessException(400)

        SseEmitter emitter = new SseEmitter(120_000L); // 2 分钟超时
        // conversationId 用于多轮记忆（M7/P1），userId 用于工具内个性化查询（M7/P2，经 ToolContext
        // 透传，不暴露给模型，避免 LLM 臆造/篡改 userId 读到别的用户的数据）
        Disposable subscription = chatService.stream(safeMessage, request.getLat(), request.getLng(),
                        request.getRadius(), request.getConversationId(), userId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.warn("AI 对话流式失败：{}", error.getMessage());
                            try {
                                emitter.send(SseEmitter.event().name("error").data("助手出错了，请稍后再试。"));
                            } catch (IOException ignored) {
                                // 客户端已断开，忽略
                            }
                            emitter.complete();
                        },
                        emitter::complete);
        // 连接终止时取消上游订阅：否则 emitter 超时/客户端断开后，boundedElastic 上的
        // LLM 阻塞调用仍会跑完（白烧 token 且占用线程）。正常完成时 dispose 为幂等 no-op。
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(t -> subscription.dispose());
        emitter.onCompletion(subscription::dispose);
        return emitter;
    }
}
