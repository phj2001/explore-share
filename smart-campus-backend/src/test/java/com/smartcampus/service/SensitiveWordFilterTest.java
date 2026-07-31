package com.smartcampus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SensitiveWordFilter} 单元测试：验证 DFA 匹配、脱敏、边界场景。
 * 词库来自 classpath:sensitive-words.txt（演示词库，含 广告骚扰/刷单/诈骗/色情/赌博/毒品/违禁品）。
 */
class SensitiveWordFilterTest {

    private SensitiveWordFilter filter;

    @BeforeEach
    void setUp() {
        // 直接 new 并手动调 init()（@PostConstruct 在单元测试中不会自动触发）
        filter = new SensitiveWordFilter();
        filter.init();
    }

    @Test
    void 命中敏感词应脱敏为星号() {
        String result = filter.clean("这是一个广告骚扰信息");
        assertThat(result).contains("***");
        assertThat(result).doesNotContain("广告骚扰");
    }

    @Test
    void 无敏感词原样返回() {
        assertThat(filter.clean("这是一个正常文本")).isEqualTo("这是一个正常文本");
    }

    @Test
    void 多个敏感词都被脱敏() {
        String result = filter.clean("内容包含诈骗和赌博字眼");
        assertThat(result).doesNotContain("诈骗");
        assertThat(result).doesNotContain("赌博");
    }

    @Test
    void null与空文本原样返回() {
        assertThat(filter.clean(null)).isNull();
        assertThat(filter.clean("")).isEqualTo("");
    }

    @Test
    void containsSensitive命中返回true() {
        assertThat(filter.containsSensitive("这里有违禁品")).isTrue();
        assertThat(filter.containsSensitive("这是正常内容")).isFalse();
    }

    @Test
    void 敏感词作为子串也能命中() {
        // DFA 是子串匹配："刷单" 在 "我是刷单员" 中应被识别
        assertThat(filter.containsSensitive("我是刷单员")).isTrue();
        assertThat(filter.clean("我是刷单员")).contains("***");
    }

    @Test
    void 脱敏后非敏感部分保留() {
        String result = filter.clean("前面正常后面毒品结尾");
        assertThat(result).startsWith("前面正常后面");
        assertThat(result).endsWith("结尾");
        assertThat(result).doesNotContain("毒品");
    }
}
