package com.smartcampus.service.impl;

import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.service.NotificationDeliveryService;
import com.smartcampus.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 通知事件发布器实现（升级项④）
 *
 * <p><b>有 MQ</b>（app.notification.outbox.rabbitmq.enabled=true 且 RabbitTemplate 可用）：
 * 发布到 RabbitMQ exchange，由 Listener 异步消费落库；消息头携带 {@code outboxId} 供消费端幂等去重。
 *
 * <p><b>无 MQ</b>（修复 #1）：rabbitTemplate=null 时<b>不再静默丢弃</b>，而是直接调用
 * {@link NotificationDeliveryService} <b>同步落库</b>——保证本地开发 / 无 MQ 部署下通知功能依然可用，
 * 且 Dispatcher 标记的 SUCCESS 名副其实。
 */
@Slf4j
@Service
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final NotificationDeliveryService deliveryService;

    public NotificationEventPublisherImpl(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            NotificationDeliveryService deliveryService,
            @Value("${app.notification.outbox.rabbitmq.enabled:true}") boolean rabbitmqEnabled,
            @Value("${app.notification.outbox.rabbitmq.exchange:discover.notification.event}") String exchange) {
        this.rabbitTemplate = rabbitmqEnabled ? rabbitTemplateProvider.getIfAvailable() : null;
        this.deliveryService = deliveryService;
        this.exchange = exchange;
    }

    @Override
    public void publish(NotificationEventOutbox event) throws Exception {
        if (rabbitTemplate == null) {
            // 无 MQ：同步落库兜底（修复 #1），携带 outboxId 幂等去重（修复 #4）
            log.debug("RabbitMQ 未启用，outbox #{} 走同步直发落库", event.getId());
            deliveryService.deliver(event.getPayloadJson(), event.getId());
            return;
        }
        String routingKey = event.getRoutingKey() != null ? event.getRoutingKey() : "notification.event";
        final Long outboxId = event.getId();
        rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayloadJson(), message -> {
            message.getMessageProperties().setHeader("outboxId", outboxId);
            return message;
        });
        log.debug("outbox #{} 已发布到 exchange={} routingKey={}", outboxId, exchange, routingKey);
    }
}
