package com.smartcampus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.entity.Notification;
import com.smartcampus.entity.User;
import com.smartcampus.repository.NotificationRepository;
import com.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知最终落库服务（升级项④收尾，修复 #1/#4）。
 *
 * <p>把"解析事件 payload → 写 Notification 表"的逻辑抽成单一入口，供两条路径共用：
 * <ul>
 *   <li><b>有 MQ</b>：{@code NotificationEventRabbitListener} 消费消息后调用本服务；</li>
 *   <li><b>无 MQ</b>：{@code NotificationEventPublisherImpl} 在 RabbitMQ 未启用时
 *       直接调用本服务<b>同步落库</b>——避免无 MQ 时通知被静默丢弃（修复 #1）。</li>
 * </ul>
 *
 * <p><b>幂等（修复 #4）</b>：携带 outboxId 时，先查 {@code existsByOutboxId} 去重，
 * 保证 Outbox at-least-once 重复投递不会产生重复通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 解析事件 payload 并写入 Notification 表。
     *
     * @param payloadJson 事件负载（sendNotification 写入 outbox 时构造）
     * @param outboxId    来源 outbox 记录 ID，用于幂等去重；可为 null（无去重）
     * @throws Exception 解析或落库失败时抛出，由调用方决定重试 / DLX
     */
    @Transactional
    public void deliver(String payloadJson, Long outboxId) throws Exception {
        // 幂等：同一 outbox 事件已落库则跳过（防 at-least-once 重复投递）
        if (outboxId != null && notificationRepository.existsByOutboxId(outboxId)) {
            log.debug("通知事件已投递，跳过重复 outboxId={}", outboxId);
            return;
        }

        JsonNode node = objectMapper.readTree(payloadJson);
        Long recipientId = node.get("recipientId").asLong();
        Long actorId = node.hasNonNull("actorId") ? node.get("actorId").asLong() : null;
        String type = node.get("type").asText();
        String title = node.get("title").asText();
        String content = node.hasNonNull("content") ? node.get("content").asText() : null;
        String targetType = node.hasNonNull("targetType") ? node.get("targetType").asText() : null;
        Long targetId = node.hasNonNull("targetId") ? node.get("targetId").asLong() : null;

        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            // 与原 Listener 行为一致：接收方不存在视为"已处理"，不抛异常（避免无意义重试）
            log.warn("通知投递：接收方不存在 recipientId={}", recipientId);
            return;
        }
        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setOutboxId(outboxId);
        notificationRepository.save(notification);
        log.debug("通知落库成功 recipientId={} type={} outboxId={}", recipientId, type, outboxId);
    }
}
