package com.smartcampus.service.impl;

import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.repository.NotificationEventOutboxRepository;
import com.smartcampus.service.NotificationEventPublisher;
import com.smartcampus.service.NotificationOutboxDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 通知 Outbox 投递调度器实现（升级项④）
 *
 * <p>来源：智慧校园 NotificationOutboxDispatcherServiceImpl（MyBatis → JPA 转写）。
 * 三态状态机 PENDING(0)/SUCCESS(1)/FAILED(2)；批量 batchSize；失败 nextRetryAt=now+retryDelay；
 * lastError 截断 500。@Scheduled 定时扫描（依赖主启动类 @EnableScheduling）。
 *
 * <p><b>事务说明（修复 #5）</b>：不在批量方法上加 @Transactional——批量方法由 @Scheduled 自调用，
 * 类内自调用不会触发事务代理，加了也无效、反而误导。每条事件通过 {@code repository.save(event)}
 * <b>独立提交</b>（无环绕事务时 Spring Data 每次 save 自带事务），正是 Outbox 期望的"逐条提交、
 * 一条失败不影响其它"语义。
 *
 * <p><b>失败自动重试（修复 #6）</b>：定时任务除扫描 PENDING 外，还会重试「未超最大次数且已到重试时间」
 * 的 FAILED 事件；超过 maxAttempts 的事件留作死信，由管理接口人工处理，避免无限重试风暴。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxDispatcherServiceImpl implements NotificationOutboxDispatcherService {

    private final NotificationEventOutboxRepository outboxRepository;
    private final NotificationEventPublisher publisher;

    @Value("${app.notification.outbox.dispatcher-enabled:true}")
    private boolean dispatcherEnabled;

    @Value("${app.notification.outbox.batch-size:20}")
    private int batchSize;

    @Value("${app.notification.outbox.retry-delay-seconds:120}")
    private long retryDelaySeconds;

    @Value("${app.notification.outbox.max-attempts:5}")
    private int maxAttempts;

    @Override
    public int dispatchPendingEvents() {
        if (!dispatcherEnabled) {
            return 0;
        }
        List<NotificationEventOutbox> pending = outboxRepository.findPendingForDispatch(
                NotificationEventOutbox.STATUS_PENDING,
                OffsetDateTime.now(),
                PageRequest.of(0, Math.max(batchSize, 1)));
        int success = 0;
        for (NotificationEventOutbox event : pending) {
            if (dispatchSingleEvent(event)) {
                success++;
            }
        }
        return success;
    }

    @Override
    public boolean retryEvent(Long outboxId) {
        if (outboxId == null) {
            throw new IllegalArgumentException("Outbox 记录ID不能为空");
        }
        NotificationEventOutbox event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox 记录不存在"));
        event.setDeliveryStatus(NotificationEventOutbox.STATUS_PENDING);
        event.setLastError(null);
        event.setNextRetryAt(OffsetDateTime.now());
        event.setPublishedAt(null);
        outboxRepository.save(event);
        return dispatchSingleEvent(event);
    }

    @Override
    public int retryFailedEvents() {
        if (!dispatcherEnabled) {
            return 0;
        }
        List<NotificationEventOutbox> failed = outboxRepository.findFailedForRetry(
                NotificationEventOutbox.STATUS_FAILED,
                maxAttempts,
                PageRequest.of(0, Math.max(batchSize, 1)));
        return redispatch(failed);
    }

    /**
     * 定时扫描投递 PENDING + 自动重试到期的 FAILED（依赖 @EnableScheduling）。
     */
    @Scheduled(fixedDelayString = "${app.notification.outbox.dispatch-interval-ms:15000}")
    public void scheduledDispatch() {
        if (!dispatcherEnabled) {
            return;
        }
        int sent = dispatchPendingEvents();

        // 修复 #6：自动重试「未超上限且到点」的失败事件
        List<NotificationEventOutbox> retryable = outboxRepository.findRetryableFailed(
                NotificationEventOutbox.STATUS_FAILED,
                maxAttempts,
                OffsetDateTime.now(),
                PageRequest.of(0, Math.max(batchSize, 1)));
        int retried = redispatch(retryable);

        if (sent > 0 || retried > 0) {
            log.info("outbox 定时投递：新发 {} 条，重试成功 {} 条", sent, retried);
        }
    }

    /** 将一批事件置回 PENDING 并尝试投递（用于手动/自动重试）。 */
    private int redispatch(List<NotificationEventOutbox> events) {
        int success = 0;
        for (NotificationEventOutbox event : events) {
            event.setDeliveryStatus(NotificationEventOutbox.STATUS_PENDING);
            event.setLastError(null);
            event.setNextRetryAt(OffsetDateTime.now());
            event.setPublishedAt(null);
            outboxRepository.save(event);
            if (dispatchSingleEvent(event)) {
                success++;
            }
        }
        return success;
    }

    private boolean dispatchSingleEvent(NotificationEventOutbox event) {
        if (event == null) {
            return false;
        }
        try {
            publisher.publish(event);
            event.setDeliveryStatus(NotificationEventOutbox.STATUS_SUCCESS);
            event.setAttemptCount((event.getAttemptCount() == null ? 0 : event.getAttemptCount()) + 1);
            event.setLastError(null);
            event.setPublishedAt(OffsetDateTime.now());
            event.setNextRetryAt(null);
            outboxRepository.save(event);
            return true;
        } catch (Exception ex) {
            int attempts = (event.getAttemptCount() == null ? 0 : event.getAttemptCount()) + 1;
            event.setDeliveryStatus(NotificationEventOutbox.STATUS_FAILED);
            event.setAttemptCount(attempts);
            event.setLastError(trimMessage(ex.getMessage()));
            event.setNextRetryAt(OffsetDateTime.now().plusSeconds(Math.max(retryDelaySeconds, 30)));
            event.setPublishedAt(null);
            outboxRepository.save(event);
            if (attempts >= maxAttempts) {
                log.error("outbox #{} 投递失败已达上限 {} 次，转入死信待人工处理：{}",
                        event.getId(), maxAttempts, event.getLastError());
            }
            return false;
        }
    }

    private String trimMessage(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
