package com.smartcampus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recommended_routes")
public class RecommendedRoute {

    public static final short STATUS_DRAFT = 0;
    public static final short STATUS_PUBLISHED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recommended_routes_id_seq")
    @SequenceGenerator(name = "recommended_routes_id_seq", sequenceName = "recommended_routes_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "summary", nullable = false, length = 220)
    private String summary;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation_text", length = 100)
    private String recommendationText;

    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "default_mode", nullable = false, length = 20)
    private String defaultMode;

    @Column(name = "status", nullable = false)
    private Short status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendedRouteWaypoint> waypoints = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (sortOrder == null) {
            sortOrder = 1;
        }
        if (defaultMode == null || defaultMode.isBlank()) {
            defaultMode = "walking";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (sortOrder == null) {
            sortOrder = 1;
        }
        if (defaultMode == null || defaultMode.isBlank()) {
            defaultMode = "walking";
        }
    }
}
