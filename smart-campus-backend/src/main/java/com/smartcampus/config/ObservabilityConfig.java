package com.smartcampus.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 可观测性配置（升级项①）
 *
 * <p>来源：智慧校园 ObservabilityConfig。适配点：
 * <ul>
 *   <li>删除 token-blacklist / session-governance / menu-cache / rabbitmq 等智慧校园专属业务标志
 *       —— 本项目无对应配置键，原样引用会导致 @Value 启动失败。</li>
 *   <li>module 通用标签改为 {@code discover}（地点探索业务域），用于 Prometheus 指标维度筛选。</li>
 *   <li><b>SB4 包路径迁移</b>：{@code MeterRegistryCustomizer} 在 SB4 从
 *       {@code org.springframework.boot.actuate.autoconfigure.metrics} 迁移到
 *       {@code org.springframework.boot.micrometer.metrics.autoconfigure}（新模块
 *       spring-boot-micrometer-metrics）。</li>
 * </ul>
 */
@Configuration
public class ObservabilityConfig {

    private static final String MODULE_TAG = "discover";

    /**
     * 为所有指标注入 application / module 通用标签，便于 Grafana 按 application 维度聚合。
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCommonTags(
            @Value("${spring.application.name:smart-campus-backend}") String applicationName) {
        return registry -> registry.config().commonTags(
                "application", applicationName,
                "module", MODULE_TAG);
    }

    /**
     * 暴露项目元信息到 /actuator/info，便于运维确认部署版本与能力清单。
     */
    @Bean
    public InfoContributor discoverInfoContributor(
            @Value("${spring.application.name:smart-campus-backend}") String applicationName) {
        return builder -> builder.withDetail("discover", Map.of(
                "application", applicationName,
                "capabilities", List.of("observability", "traceId", "postgis-spatial"),
                "runtimeProfile", Locale.getDefault().toLanguageTag()
        ));
    }
}
