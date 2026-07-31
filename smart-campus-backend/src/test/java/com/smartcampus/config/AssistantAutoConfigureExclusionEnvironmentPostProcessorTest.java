package com.smartcampus.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link AssistantAutoConfigureExclusionEnvironmentPostProcessor}：
 * <ol>
 *   <li>被 spring.factories 正确注册、SpringFactoriesLoader 能发现（注册 key 必须用 SB4 新 FQN
 *       {@code org.springframework.boot.EnvironmentPostProcessor}，旧 {@code .env} 路径在 SB4 仅作 deprecated 桥接）;</li>
 *   <li>assistant 关闭时注入 {@code spring.autoconfigure.exclude}，含全部 OpenAi*AutoConfiguration；</li>
 *   <li>assistant 开启时不注入（保留 autoconfigure 正常装配，此时 api-key 必须配真实值）；</li>
 *   <li>未设开关时按默认关闭处理。</li>
 * </ol>
 * 该测试不启动 Spring 上下文，是纯单元测试，无需 DB/Redis/MQ。
 */
class AssistantAutoConfigureExclusionEnvironmentPostProcessorTest {

    private final AssistantAutoConfigureExclusionEnvironmentPostProcessor processor =
            new AssistantAutoConfigureExclusionEnvironmentPostProcessor();

    /** 核心：验证 spring.factories 注册 key 正确，SB4 的 SpringFactoriesLoader 能发现本处理器。
     *  用 loadFactoryNames 只读类名、不实例化——loadFactories 会实例化所有 factory，
     *  连带触发 SB4 内置 processor(如 CloudFoundryVcapEnvironmentPostProcessor)的构造器依赖而失败。 */
    @Test
    void 应被SpringFactoriesLoader发现() {
        List<String> names = SpringFactoriesLoader.loadFactoryNames(
                EnvironmentPostProcessor.class, getClass().getClassLoader());
        assertThat(names).contains(
                AssistantAutoConfigureExclusionEnvironmentPostProcessor.class.getName());
    }

    @Test
    void assistant关闭时排除SpringAiAutoConfigure() {
        ConfigurableEnvironment env = envWith("app.assistant.enabled", "false");
        processor.postProcessEnvironment(env, null);
        String exclude = env.getProperty("spring.autoconfigure.exclude");
        assertThat(exclude).isNotNull();
        assertThat(exclude).contains("OpenAiChatAutoConfiguration");
        assertThat(exclude).contains("OpenAiEmbeddingAutoConfiguration");
        assertThat(exclude).contains("OpenAiImageAutoConfiguration");
    }

    @Test
    void assistant开启时不排除() {
        ConfigurableEnvironment env = envWith("app.assistant.enabled", "true");
        processor.postProcessEnvironment(env, null);
        // 开启时不注入 spring.autoconfigure.exclude，保留 autoconfigure 正常装配
        assertThat(env.getProperty("spring.autoconfigure.exclude")).isNull();
    }

    @Test
    void 未设开关时默认视为关闭() {
        ConfigurableEnvironment env = new StandardEnvironment();
        processor.postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.autoconfigure.exclude"))
                .contains("OpenAiChatAutoConfiguration");
    }

    @Test
    void 合并既有exclude避免覆盖用户自定义() {
        ConfigurableEnvironment env = envWith("spring.autoconfigure.exclude",
                "com.example.FooAutoConfiguration");
        processor.postProcessEnvironment(env, null);
        String exclude = env.getProperty("spring.autoconfigure.exclude");
        assertThat(exclude).contains("com.example.FooAutoConfiguration");
        assertThat(exclude).contains("OpenAiChatAutoConfiguration");
    }

    private ConfigurableEnvironment envWith(String key, String value) {
        ConfigurableEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(key, value)));
        return env;
    }
}
