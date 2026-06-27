package com.smartcampus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.NotificationResponse;
import com.smartcampus.entity.Notification;
import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.repository.NotificationEventOutboxRepository;
import com.smartcampus.repository.NotificationRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional  // 升级项④：默认 REQUIRED，与业务同事务（Outbox 模式核心——业务回滚则事件回滚）
    public void sendNotification(Long recipientId, Long actorId, String type, String title, String content, String targetType, Long targetId) {
        // 保留原"不给自己发通知"守卫
        if (recipientId.equals(actorId)) return;
        if (userRepository.findById(recipientId).isEmpty()) return;

        // 写 outbox 事件（业务成功则事件必存；业务回滚则事件回滚）
        NotificationEventOutbox event = new NotificationEventOutbox();
        event.setEventType(type);
        event.setAggregateType(targetType);
        event.setAggregateId(targetId);
        event.setRoutingKey(buildRoutingKey(type));
        event.setPayloadJson(buildPayloadJson(recipientId, actorId, type, title, content, targetType, targetId));
        event.setDeliveryStatus(NotificationEventOutbox.STATUS_PENDING);
        event.setAttemptCount(0);
        outboxRepository.save(event);
        // 真正的 Notification 落库交给 RabbitMQ Listener 异步完成
    }

    private String buildPayloadJson(Long recipientId, Long actorId, String type, String title, String content, String targetType, Long targetId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("recipientId", recipientId);
        if (actorId != null) node.put("actorId", actorId);
        node.put("type", type);
        node.put("title", title);
        if (content != null) node.put("content", content);
        if (targetType != null) node.put("targetType", targetType);
        if (targetId != null) node.put("targetId", targetId);
        return node.toString();
    }

    private String buildRoutingKey(String type) {
        return type != null ? "notification." + type.toLowerCase() : "notification.event";
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> result = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> records = result.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                n.getActor() != null ? n.getActor().getId() : null,
                n.getActor() != null ? n.getActor().getDisplayName() : null,
                n.getActor() != null ? n.getActor().getAvatarUrl() : null,
                n.getTargetType(),
                n.getTargetId(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt()
        );
    }
}
