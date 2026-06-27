package com.smartcampus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admin_operation_logs")
public class AdminOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_operation_logs_id_seq")
    @SequenceGenerator(name = "admin_operation_logs_id_seq", sequenceName = "admin_operation_logs_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "operator_user_id", nullable = false)
    private Long operatorUserId;

    @Column(name = "operator_display_name", nullable = false, length = 100)
    private String operatorDisplayName;

    @Column(name = "operator_username", nullable = false, length = 50)
    private String operatorUsername;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;

    @Column(name = "action_name", nullable = false, length = 80)
    private String actionName;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "summary", nullable = false, length = 300)
    private String summary;

    // ----- 升级项② AOP 审计增强字段（允许 null，向后兼容历史数据）-----
    /** HTTP 方法（GET/POST/PUT/DELETE） */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /** 请求 URI */
    @Column(name = "request_uri", length = 200)
    private String requestUri;

    /** 客户端 IP */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** 操作状态：1=成功, 0=失败 */
    @Column(name = "operation_status")
    private Integer operationStatus;

    /** 链路追踪 ID（与日志/指标串联） */
    @Column(name = "trace_id", length = 40)
    private String traceId;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
