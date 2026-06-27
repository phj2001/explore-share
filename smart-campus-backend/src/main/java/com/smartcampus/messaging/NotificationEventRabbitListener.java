package com.smartcampus.messaging;

import com.smartcampus.service.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 通知事件 RabbitMQ 消费者（升级项④）
 *
 * <p>消费 outbox 发布的事件，委托 {@link NotificationDeliveryService} 反序列化并写入 Notification 表。
 * 消费失败抛异常，经 retry 超限后由队列的 DLX 配置路由到死信队列，避免无限重试。
 * 通过消息头 {@code outboxId} 做幂等去重（修复 #4）。
 *
 * <p>@ConditionalOnProperty：RabbitMQ 未启用时不启动监听容器（此时由 Publisher 同步直发，见 #1 修复）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification.outbox.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationEventRabbitListener {

    private final NotificationDeliveryService deliveryService;

    @RabbitListener(queues = "${app.notification.outbox.rabbitmq.queue:discover.notification.event.queue}")
    public void onNotificationEvent(@Payload String payloadJson,
                                    @Header(name = "outboxId", required = false) Long outboxId) {
        try {
            deliveryService.deliver(payloadJson, outboxId);
            log.debug("通知事件消费成功 outboxId={}", outboxId);
        } catch (Exception ex) {
            log.error("通知事件消费失败 outboxId={} payload={}", outboxId, payloadJson, ex);
            // 抛异常触发 retry，超限后由 DLX 路由到死信队列
            throw new RuntimeException("通知事件消费失败", ex);
        }
    }
}
