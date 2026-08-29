package com.smartcampus.service.achievement;

import java.util.Map;
import java.util.Optional;

/**
 * 成就规则表：成就 id → （指标, 阈值），解锁判定与进度计算的唯一数据源。
 * 成就定义（名称/图标/排序）是 DB 手工维护数据，此处的 id 与 achievement_definitions.id 为强约定——
 * 不在表内的 id 永不解锁、也不显示进度条；新增成就须同步本表与 DB 定义。
 */
public final class AchievementRules {

    private AchievementRules() {
    }

    public enum Metric { CHECK_IN, SHARE, RECEIVED_LIKE, DISTINCT_CATEGORY, ROUTE }

    public record Rule(Metric metric, long threshold) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            "check_in_1", new Rule(Metric.CHECK_IN, 1),
            "check_in_10", new Rule(Metric.CHECK_IN, 10),
            "check_in_50", new Rule(Metric.CHECK_IN, 50),
            "check_in_100", new Rule(Metric.CHECK_IN, 100),
            "share_1", new Rule(Metric.SHARE, 1),
            "share_10", new Rule(Metric.SHARE, 10),
            "likes_10", new Rule(Metric.RECEIVED_LIKE, 10),
            "likes_100", new Rule(Metric.RECEIVED_LIKE, 100),
            "category_5", new Rule(Metric.DISTINCT_CATEGORY, 5),
            "route_1", new Rule(Metric.ROUTE, 1)
    );

    public static Optional<Rule> resolve(String achievementId) {
        return Optional.ofNullable(RULES.get(achievementId));
    }

    public static Map<String, Rule> all() {
        return RULES;
    }
}
