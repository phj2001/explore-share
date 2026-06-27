package com.smartcampus.service;

import com.smartcampus.entity.NotificationEventOutbox;

/**
 * 通知事件发布器（升级项④）
 *
 * <p>由 Dispatcher 调用，将 outbox 事件发布到 RabbitMQ exchange。
 * 实现支持 mock 降级（RabbitMQ 未启用时不真正发布）。
 */
public interface NotificationEventPublisher {

    /**
     * 发布 outbox 事件。失败时抛异常，由 Dispatcher 捕获并标记 FAILED + 设置重试时间。
     */
    void publish(NotificationEventOutbox event) throws Exception;
}
