package com.smartcampus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "content_reports")
public class ContentReport {

    public static final short TARGET_TYPE_SHARE = 1;
    public static final short TARGET_TYPE_REPLY = 2;

    public static final short REASON_SPAM = 1;
    public static final short REASON_INAPPROPRIATE = 2;
    public static final short REASON_FALSE_INFO = 3;
    public static final short REASON_ABUSE = 4;
    public static final short REASON_OTHER = 5;

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_PROCESSED = 2;
    public static final short STATUS_REJECTED = 3;

    public static final short ACTION_NONE = 0;
    public static final short ACTION_DELETE_TARGET = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "content_reports_id_seq")
    @SequenceGenerator(name = "content_reports_id_seq", sequenceName = "content_reports_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "target_type", nullable = false)
    private Short targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "related_share_id")
    private Long relatedShareId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reason_code", nullable = false)
    private Short reasonCode;

    @Column(name = "reason_detail", length = 200)
    private String reasonDetail;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_PENDING;

    @Column(name = "review_action")
    private Short reviewAction;

    @Column(name = "review_note", length = 200)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "target_content_preview", nullable = false, length = 300)
    private String targetContentPreview;

    @Column(name = "target_author_user_id", nullable = false)
    private Long targetAuthorUserId;

    @Column(name = "target_author_display_name", nullable = false, length = 100)
    private String targetAuthorDisplayName;

    @Column(name = "target_author_username", nullable = false, length = 50)
    private String targetAuthorUsername;

    @Column(name = "target_poi_name", length = 100)
    private String targetPoiName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = STATUS_PENDING;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
