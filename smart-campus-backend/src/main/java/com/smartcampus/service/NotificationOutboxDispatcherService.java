package com.smartcampus.service;

/**
 * 通知 Outbox 投递调度器（升级项④）
 *
 * <p>定时扫描 PENDING 事件发布到 RabbitMQ；提供手动重试单条/批量失败事件的能力。
 */
public interface NotificationOutboxDispatcherService {

    /** 投递所有待发事件，返回成功条数 */
    int dispatchPendingEvents();

    /** 手动重试单条事件 */
    boolean retryEvent(Long outboxId);

    /** 手动重试所有失败事件，返回成功条数 */
    int retryFailedEvents();
}
