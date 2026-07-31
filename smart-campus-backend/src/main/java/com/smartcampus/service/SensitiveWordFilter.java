package com.smartcampus.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词过滤器：基于 DFA（确定有限自动机）的多模式匹配，UGC 文本入库前脱敏。
 *
 * <p><b>为什么 DFA</b>：敏感词词库可能有数千条。朴素做法是对每个词做 {@code String.contains}，
 * 复杂度 O(词数 × 文本长)；DFA 把词库预处理成状态机，扫描整段文本只需 O(文本长)，
 * 且命中即停，适合分享/回复/评价等高频 UGC 入库前过滤。
 *
 * <p><b>命中策略：脱敏</b>（敏感词替换为 {@code ***}）。相比直接抛异常拒绝，脱敏不阻断用户正常发布，
 * 又能保证入库内容合规；需要"命中即拒绝"的场景（如严管板块）可调用 {@link #containsSensitive} 自行处理。
 *
 * <p><b>词库</b>：{@code classpath:sensitive-words.txt}，每行一个词，{@code #} 开头为注释。
 * 默认是演示词库，生产应替换为完整词库或接入第三方敏感词服务。
 *
 * <p><b>降级策略：fail-open</b>——词库加载失败时不阻断启动、不阻断 UGC（与项目 AI 模块的 fail-open 风格一致），
 * 生产若需更严可改为 fail-closed。null/空文本原样返回。
 *
 * <p>面试考点：DFA 多模式匹配原理与复杂度、脱敏 vs 拒绝的取舍、UGC 事前审核与事后举报的闭环、fail-open 策略。
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /** 脱敏替换符 */
    private static final String REPLACEMENT = "***";
    /** DFA 节点中的终止标记 key */
    private static final String END_FLAG = "isEnd";

    /** DFA 根节点：嵌套 Map，每个 key 是一个字符，value 是子节点 Map */
    private final Map<String, Object> dfaRoot = new HashMap<>();

    @PostConstruct
    public void init() {
        Set<String> words = loadWords();
        for (String word : words) {
            if (StringUtils.hasText(word)) {
                insertWord(word);
            }
        }
        log.info("敏感词过滤器初始化完成，加载词数：{}", words.size());
    }

    /** 脱敏：把文本中的敏感词替换为 ***。null/空原样返回。 */
    public String clean(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int matchEnd = matchEnd(text, i);
            if (matchEnd > i) {
                result.append(REPLACEMENT);
                i = matchEnd;
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /** 是否包含敏感词。 */
    public boolean containsSensitive(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (matchEnd(text, i) > i) {
                return true;
            }
        }
        return false;
    }

    /** 从 start 起在 DFA 上走，返回命中的最长敏感词结束位置（exclusive）；未命中返回 start。 */
    @SuppressWarnings("unchecked")
    private int matchEnd(String text, int start) {
        Map<String, Object> current = dfaRoot;
        int matchEnd = start;
        for (int i = start; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            Map<String, Object> next = (Map<String, Object>) current.get(ch);
            if (next == null) {
                break;
            }
            current = next;
            if (Boolean.TRUE.equals(current.get(END_FLAG))) {
                matchEnd = i + 1;
            }
        }
        return matchEnd;
    }

    /** 把一个词插入 DFA（逐字符建立状态转移，末节点置 isEnd=true）。 */
    @SuppressWarnings("unchecked")
    private void insertWord(String word) {
        Map<String, Object> current = dfaRoot;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            Map<String, Object> next = (Map<String, Object>) current.get(ch);
            if (next == null) {
                next = new HashMap<>();
                current.put(ch, next);
            }
            current = next;
        }
        current.put(END_FLAG, Boolean.TRUE);
    }

    private Set<String> loadWords() {
        Set<String> words = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("sensitive-words.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    words.add(line);
                }
            }
        } catch (Exception e) {
            // fail-open：词库加载失败不阻断启动，降级为"不过滤"；记录告警便于排查
            log.warn("敏感词词库加载失败，过滤器降级为不过滤：{}", e.getMessage());
        }
        return words;
    }
}
