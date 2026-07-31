package com.smartcampus.config;

import org.springframework.boot.SpringApplication;
// SB4 迁移：EnvironmentPostProcessor 从 org.springframework.boot.env 移到 org.springframework.boot 包
// （见 Spring Boot 4.0 Migration Guide）。旧路径仅作 deprecated 桥接，spring.factories 注册 key 也须用新 FQN。
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 探索助手关闭时，动态排除 Spring AI OpenAI 自动配置，避免空 api-key 触发 autoconfigure 启动异常。
 *
 * <p>背景：{@code spring-ai-starter-model-openai} 的 {@code OpenAiChatAutoConfiguration} 等在
 * {@code spring.ai.openai.api-key} 为空时会启动失败。本项目 AI 助手默认关闭（{@code app.assistant.enabled=false}），
 * 关闭时完全不需要这些 Bean。早期用 api-key 占位串（{@code disabled-when-assistant-off}）规避，是 hack。
 *
 * <p>本类在自动配置发生之前向 Environment 注入 {@code spring.autoconfigure.exclude}：
 * 当 {@code app.assistant.enabled} 为 false（默认）时排除全部 OpenAi*AutoConfiguration，
 * 从而 api-key 可留空、无需占位串；assistant 开启时不排除，正常装配（此时 api-key 必须配真实值）。
 *
 * <p>比 {@code @SpringBootApplication(exclude=...)} 更灵活——后者是静态排除，无法按开关条件化。
 * 注册见 {@code META-INF/spring.factories}。
 *
 * <p>面试考点：Spring Boot 自动配置的条件化排除、EnvironmentPostProcessor 早于 autoconfigure 执行的时机、
 * "默认关闭即不装配"的零侵入模块设计。
 */
public class AssistantAutoConfigureExclusionEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "assistant-autoconfigure-exclusion";

    /** assistant 关闭时排除的 Spring AI OpenAI 自动配置（项目仅用 chat + embedding，其余一并排除无副作用）。 */
    private static final String[] EXCLUDED_AUTO_CONFIGURATIONS = {
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 默认关闭（与 app.assistant.enabled 默认 false 一致）
        boolean enabled = Boolean.parseBoolean(environment.getProperty("app.assistant.enabled", "false"));
        if (enabled) {
            return;
        }
        // 合并既有的 spring.autoconfigure.exclude（用户可能自定义了其它排除项，避免覆盖）
        String existing = environment.getProperty("spring.autoconfigure.exclude", "");
        String merged = StringUtils.hasText(existing)
                ? existing + "," + String.join(",", EXCLUDED_AUTO_CONFIGURATIONS)
                : String.join(",", EXCLUDED_AUTO_CONFIGURATIONS);
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.autoconfigure.exclude", merged);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }
}
