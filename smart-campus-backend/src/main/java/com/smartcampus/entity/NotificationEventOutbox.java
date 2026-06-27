package com.smartcampus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 通知事件 Outbox（升级项④）
 *
 * <p>来源：智慧校园 NotificationEventOutbox（MyBatis → JPA 转写）。
 * 通用领域事件 outbox：业务同事务写入，Dispatcher 定时扫描发布到 RabbitMQ，
 * Listener 消费后落库 Notification 表，实现通知的异步、可重试、最终一致投递。
 *
 * <p>字段取舍（文档 8.4）：保留 aggregateType/aggregateId/routingKey 支撑通用事件扩展；
 * 省略 eventLogId（本项目无事件日志体系）、topicName（单一 exchange）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_event_outbox")
public class NotificationEventOutbox {

    /** 投递状态：待发 */
    public static final int STATUS_PENDING = 0;
    /** 投递状态：成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 投递状态：失败 */
    public static final int STATUS_FAILED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_event_outbox_id_seq")
    @SequenceGenerator(name = "notification_event_outbox_id_seq", sequenceName = "notification_event_outbox_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /** 聚合根类型（如 "分享"/"回复"，对应通知 targetType） */
    @Column(name = "aggregate_type", length = 50)
    private String aggregateType;

    /** 聚合根 ID（对应通知 targetId） */
    @Column(name = "aggregate_id")
    private Long aggregateId;

    /** 生成的通知 ID（Listener 落库后回填，可空） */
    @Column(name = "notification_id")
    private Long notificationId;

    /** 事件类型（对应通知 type，如 LIKE/REPLY/FOLLOW） */
    @Column(name = "event_type", length = 30)
    private String eventType;

    /** MQ 路由键（如 notification.like） */
    @Column(name = "routing_key", length = 100)
    private String routingKey;

    /** 事件负载 JSON（recipientId/actorId/type/title/content/targetType/targetId） */
    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    /** 投递状态：0=待发, 1=成功, 2=失败 */
    @Column(name = "delivery_status", nullable = false)
    private Integer deliveryStatus;

    /** 尝试次数 */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /** 最近错误信息（截断 500） */
    @Column(name = "last_error", length = 500)
    private String lastError;

    /** 下次重试时间（null 表示立即） */
    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    /** 成功投递时间 */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
