package com.smartcampus.service.impl;

import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.repository.NotificationEventOutboxRepository;
import com.smartcampus.service.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

/**
 * Outbox Dispatcher 单元测试（升级项④ 回归）。
 * 覆盖三态状态机 PENDING→SUCCESS/FAILED、重试逻辑、开关。
 */
@ExtendWith(MockitoExtension.class)
class NotificationOutboxDispatcherServiceImplTest {

    @Mock
    private NotificationEventOutboxRepository outboxRepository;
    @Mock
    private NotificationEventPublisher publisher;

    @InjectMocks
    private NotificationOutboxDispatcherServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dispatcherEnabled", true);
        ReflectionTestUtils.setField(service, "batchSize", 20);
        ReflectionTestUtils.setField(service, "retryDelaySeconds", 120L);
    }

    @Test
    void dispatchPendingEvents_发布成功_标记SUCCESS() throws Exception {
        NotificationEventOutbox event = buildPendingEvent(1L);
        when(outboxRepository.findPendingForDispatch(eq(NotificationEventOutbox.STATUS_PENDING), any(), any()))
                .thenReturn(List.of(event));

        int count = service.dispatchPendingEvents();

        assertEquals(1, count);
        assertEquals(NotificationEventOutbox.STATUS_SUCCESS, event.getDeliveryStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(1, event.getAttemptCount());
        verify(publisher).publish(event);
        verify(outboxRepository).save(event);
    }

    @Test
    void dispatchPendingEvents_发布失败_标记FAILED并设置重试时间和错误() throws Exception {
        NotificationEventOutbox event = buildPendingEvent(1L);
        when(outboxRepository.findPendingForDispatch(eq(NotificationEventOutbox.STATUS_PENDING), any(), any()))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("RabbitMQ 连接失败")).when(publisher).publish(event);

        int count = service.dispatchPendingEvents();

        assertEquals(0, count);
        assertEquals(NotificationEventOutbox.STATUS_FAILED, event.getDeliveryStatus());
        assertNotNull(event.getNextRetryAt());
        assertTrue(event.getLastError().contains("RabbitMQ 连接失败"));
        verify(outboxRepository).save(event);
    }

    @Test
    void dispatchPendingEvents_开关关闭_返回0不查库() {
        ReflectionTestUtils.setField(service, "dispatcherEnabled", false);
        int count = service.dispatchPendingEvents();
        assertEquals(0, count);
        verify(outboxRepository, never()).findPendingForDispatch(anyInt(), any(), any());
    }

    @Test
    void retryEvent_存在_重置PENDING并重试成功() throws Exception {
        NotificationEventOutbox event = buildPendingEvent(1L);
        event.setDeliveryStatus(NotificationEventOutbox.STATUS_FAILED);
        event.setLastError("旧错误");
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(event));

        boolean result = service.retryEvent(1L);

        assertTrue(result);
        assertEquals(NotificationEventOutbox.STATUS_SUCCESS, event.getDeliveryStatus());
    }

    @Test
    void retryEvent_不存在_抛IllegalArgumentException() {
        when(outboxRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.retryEvent(99L));
    }

    @Test
    void retryEvent_id为null_抛异常() {
        assertThrows(IllegalArgumentException.class, () -> service.retryEvent(null));
    }

    @Test
    void retryFailedEvents_批量重置重试() throws Exception {
        NotificationEventOutbox e1 = buildPendingEvent(1L);
        NotificationEventOutbox e2 = buildPendingEvent(2L);
        when(outboxRepository.findFailedForRetry(eq(NotificationEventOutbox.STATUS_FAILED), any()))
                .thenReturn(List.of(e1, e2));

        int count = service.retryFailedEvents();

        assertEquals(2, count);
        verify(publisher).publish(e1);
        verify(publisher).publish(e2);
    }

    @Test
    void retryFailedEvents_开关关闭_返回0() {
        ReflectionTestUtils.setField(service, "dispatcherEnabled", false);
        assertEquals(0, service.retryFailedEvents());
        verify(outboxRepository, never()).findFailedForRetry(anyInt(), any());
    }

    private NotificationEventOutbox buildPendingEvent(Long id) {
        NotificationEventOutbox e = new NotificationEventOutbox();
        e.setId(id);
        e.setEventType("LIKE");
        e.setDeliveryStatus(NotificationEventOutbox.STATUS_PENDING);
        e.setAttemptCount(0);
        e.setPayloadJson("{\"recipientId\":1,\"type\":\"LIKE\",\"title\":\"点赞\"}");
        return e;
    }
}
