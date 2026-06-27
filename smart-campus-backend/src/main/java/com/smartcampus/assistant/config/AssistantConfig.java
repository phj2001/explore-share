package com.smartcampus.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 探索助手 Spring 配置入口。
 *
 * <p>开关：仅当 {@code app.assistant.enabled=true} 时装配。配合 {@code AssistantController}
 * 上的同名 {@link ConditionalOnProperty}，实现"一键关闭即整体回退到纯平台"（任务规则 6 / 方案 §13）。
 *
 * <p>{@link ChatClient} 基于 Spring AI 自动装配的 {@link ChatClient.Builder}（其底层
 * OpenAiChatModel 由 {@code spring-ai-starter-model-openai} 的 autoconfigure，
 * 经 {@code spring.ai.openai.*} 指向 DashScope OpenAI 兼容端点）。
 * 系统提示词、工具（{@code @Tool}）绑定、advisor 在 M3 补充。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantConfig {

    /**
     * 最小 ChatClient：M0 仅装配与同步调用验证。流式（{@code .stream()} → SSE 桥接）在 M3。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
