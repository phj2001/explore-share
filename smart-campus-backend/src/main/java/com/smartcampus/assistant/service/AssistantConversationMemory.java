package com.smartcampus.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.assistant.config.AssistantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AI 探索助手 · 多轮对话记忆（M7，升级方案 P1）。
 *
 * <p>用 Redis List 按 conversationId 保存最近若干轮的用户/助手消息（JSON 编码），供
 * {@link AssistantChatService} 组装成 {@link Message} 列表传给
 * {@link org.springframework.ai.chat.client.ChatClient}，使"这个""再远一点"这类指代性追问
 * 能结合上文正确理解真实意图。
 *
 * <p>存储选型：会话记忆是短期、可丢失的（TTL 内有效即可，不需要跨设备持久化查询），复用项目
 * 既有 Redis（与限流同套设施），不新增数据库表。
 *
 * <p><b>容错（fail-open）</b>：读写异常均只记日志、不影响主对话——最坏情况是退化为单轮对话，
 * 而不是让整个请求失败。conversationId 由前端在打开对话面板时生成（UUID），未传时视为
 * 无历史的单轮请求。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
public class AssistantConversationMemory {

    private static final String KEY_PREFIX = "assistant:conv:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AssistantProperties properties;

    public AssistantConversationMemory(StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        AssistantProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 读取某会话最近的历史消息（按时间正序，user/assistant 交替）。
     * conversationId 为空、历史记忆关闭、或 Redis 异常均返回空列表（等同单轮对话）。
     */
    public List<Message> loadHistory(String conversationId) {
        if (!properties.getHistory().isEnabled() || conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        try {
            String key = KEY_PREFIX + conversationId;
            int maxEntries = properties.getHistory().getMaxTurns() * 2;
            List<String> raw = redisTemplate.opsForList().range(key, -maxEntries, -1);
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<Message> messages = new ArrayList<>(raw.size());
            for (String line : raw) {
                parseLine(line).ifPresent(messages::add);
            }
            return messages;
        } catch (Exception e) {
            log.warn("对话历史读取失败（视为无历史）：{}", e.getMessage());
            return List.of();
        }
    }

    /** 追加一轮消息（user 或 assistant），并续期 TTL。conversationId 为空时静默忽略。 */
    public void append(String conversationId, String role, String content) {
        if (!properties.getHistory().isEnabled() || conversationId == null || conversationId.isBlank()
                || content == null || content.isBlank()) {
            return;
        }
        try {
            String key = KEY_PREFIX + conversationId;
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(new Turn(role, content)));
            // 只保留最近 maxTurns 轮（一问一答 = 2 条），防止单会话无限膨胀
            int maxEntries = properties.getHistory().getMaxTurns() * 2;
            redisTemplate.opsForList().trim(key, -maxEntries, -1);
            redisTemplate.expire(key, Duration.ofMinutes(properties.getHistory().getTtlMinutes()));
        } catch (Exception e) {
            log.warn("对话历史写入失败（忽略）：{}", e.getMessage());
        }
    }

    private Optional<Message> parseLine(String line) {
        try {
            Turn turn = objectMapper.readValue(line, Turn.class);
            if ("user".equals(turn.role())) {
                return Optional.of(new UserMessage(turn.content()));
            } else if ("assistant".equals(turn.role())) {
                return Optional.of(new AssistantMessage(turn.content()));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("对话历史记录解析失败，跳过该条：{}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 单轮消息的存储结构（role: "user" | "assistant"）。 */
    private record Turn(String role, String content) {
    }
}
