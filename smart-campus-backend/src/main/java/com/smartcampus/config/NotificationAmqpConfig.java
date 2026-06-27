package com.smartcampus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知事件 RabbitMQ 配置（升级项④）
 *
 * <p>来源：智慧校园 NotificationAmqpConfig，仅改 exchange/queue 前缀（值由 application.properties 注入）。
 * 主队列绑定死信交换机（DLX），消费失败重试超限后自动路由到死信队列（DLQ），避免无限重试。
 *
 * <p>@ConditionalOnProperty：RabbitMQ 未启用（本地开发）时不创建 AMQP bean，避免启动连接失败。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.notification.outbox.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationAmqpConfig {

    @Bean
    public TopicExchange notificationEventExchange(
            @Value("${app.notification.outbox.rabbitmq.exchange:discover.notification.event}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange notificationEventDeadLetterExchange(
            @Value("${app.notification.outbox.rabbitmq.dead-letter-exchange:discover.notification.event.dlx}") String dlxName) {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue notificationEventQueue(
            @Value("${app.notification.outbox.rabbitmq.queue:discover.notification.event.queue}") String queueName,
            @Value("${app.notification.outbox.rabbitmq.dead-letter-exchange:discover.notification.event.dlx}") String dlxName,
            @Value("${app.notification.outbox.rabbitmq.dead-letter-routing-key:notification.dead}") String dlRoutingKey) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxName);
        args.put("x-dead-letter-routing-key", dlRoutingKey);
        return new Queue(queueName, true, false, false, args);
    }

    @Bean
    public Queue notificationEventDeadLetterQueue(
            @Value("${app.notification.outbox.rabbitmq.dead-letter-queue:discover.notification.event.dlq}") String dlqName) {
        return new Queue(dlqName, true);
    }

    @Bean
    public Binding notificationEventBinding(
            Queue notificationEventQueue,
            TopicExchange notificationEventExchange,
            @Value("${app.notification.outbox.rabbitmq.binding-routing-key:notification.#}") String bindingKey) {
        return BindingBuilder.bind(notificationEventQueue).to(notificationEventExchange).with(bindingKey);
    }

    @Bean
    public Binding notificationEventDeadLetterBinding(
            Queue notificationEventDeadLetterQueue,
            TopicExchange notificationEventDeadLetterExchange,
            @Value("${app.notification.outbox.rabbitmq.dead-letter-routing-key:notification.dead}") String dlRoutingKey) {
        return BindingBuilder.bind(notificationEventDeadLetterQueue).to(notificationEventDeadLetterExchange).with(dlRoutingKey);
    }
}
