package com.smartcampus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.repository.NotificationEventOutboxRepository;
import com.smartcampus.repository.NotificationRepository;
import com.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * NotificationServiceImpl.sendNotification 单元测试（升级项④ 回归）。
 * 聚焦：守卫保留（自收/接收方不存在）+ 写 outbox（PENDING/payload）+ 不再同步写 Notification。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationEventOutboxRepository outboxRepository;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        // 手动构造，确保 ObjectMapper 为真实实例（buildPayloadJson 需真实序列化）
        service = new NotificationServiceImpl(notificationRepository, userRepository, outboxRepository, new ObjectMapper());
    }

    @Test
    void sendNotification_给自己_不写outbox() {
        service.sendNotification(1L, 1L, "LIKE", "title", "content", "分享", 10L);
        verifyNoInteractions(outboxRepository);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotification_接收方不存在_不写outbox() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        service.sendNotification(2L, 1L, "LIKE", "title", "content", "分享", 10L);
        verify(outboxRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotification_正常_写outbox_PENDING且payload完整() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(new com.smartcampus.entity.User()));
        service.sendNotification(2L, 1L, "LIKE", "点赞了你的分享", "内容", "分享", 10L);

        ArgumentCaptor<NotificationEventOutbox> captor = ArgumentCaptor.forClass(NotificationEventOutbox.class);
        verify(outboxRepository).save(captor.capture());
        NotificationEventOutbox saved = captor.getValue();

        assertEquals(NotificationEventOutbox.STATUS_PENDING, saved.getDeliveryStatus());
        assertEquals("LIKE", saved.getEventType());
        assertEquals("分享", saved.getAggregateType());
        assertEquals(10L, saved.getAggregateId());
        assertTrue(saved.getRoutingKey().contains("like"));
        assertTrue(saved.getPayloadJson().contains("\"recipientId\":2"));
        assertTrue(saved.getPayloadJson().contains("\"actorId\":1"));
        assertTrue(saved.getPayloadJson().contains("点赞了你的分享"));
        // 核心断言：不再同步直写 Notification 表（交给 Listener 异步）
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendNotification_actor为null_payload不含actorId() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(new com.smartcampus.entity.User()));
        service.sendNotification(2L, null, "SYSTEM", "系统通知", null, null, null);

        ArgumentCaptor<NotificationEventOutbox> captor = ArgumentCaptor.forClass(NotificationEventOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertTrue(captor.getValue().getPayloadJson().contains("\"recipientId\":2"));
        assertTrue(!captor.getValue().getPayloadJson().contains("actorId"));
    }
}
