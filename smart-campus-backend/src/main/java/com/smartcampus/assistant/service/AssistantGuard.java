package com.smartcampus.assistant.service;

import com.smartcampus.assistant.config.AssistantProperties;
import com.smartcampus.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 探索助手 · 输入护栏（M4，方案 §8）。
 *
 * <p>对用户消息做长度限制 + 常见 Prompt 注入/越权启发式检测。命中即拒绝（{@link BusinessException} 400）。
 *
 * <p>定位：这是<b>工程化的基础防线</b>（轻量启发式），并非绝对安全；与系统提示词约束、
 * 工具只返回平台真实数据共同构成纵深防御。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantGuard {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("忽略(以上|之前|前面|上面).{0,6}(指令|提示|规则|设定)"),
            Pattern.compile("(?i)ignore\\s+(the\\s+)?(previous|above|prior|all)\\s+(instructions?|prompts?)"),
            Pattern.compile("(?i)system\\s+prompt"),
            Pattern.compile("(?i)(developer|debug)\\s+mode"),
            Pattern.compile("开发者模式|越狱|jailbreak"),
            Pattern.compile("(?i)you\\s+are\\s+now"),
            Pattern.compile("(忘记|无视).{0,6}(你的)?(身份|角色|设定)")
    );

    private final AssistantProperties properties;

    /**
     * 校验并返回清洗后的消息（去首尾空白）。违规抛 {@link BusinessException}。
     */
    public String check(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(400, "消息不能为空");
        }
        String trimmed = message.strip();
        int max = properties.getGuard().getMaxMessageLength();
        if (trimmed.length() > max) {
            throw new BusinessException(400, "消息过长（最多 " + max + " 字）");
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(trimmed).find()) {
                throw new BusinessException(400, "消息包含不被允许的内容");
            }
        }
        return trimmed;
    }
}
