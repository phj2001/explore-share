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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
