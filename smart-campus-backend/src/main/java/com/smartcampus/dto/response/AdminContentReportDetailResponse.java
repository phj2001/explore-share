package com.smartcampus.dto.response;

import com.smartcampus.entity.ContentReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentReportDetailResponse {

    private Long id;
    private Short targetType;
    private Long targetId;
    private Long relatedShareId;
    private Short reasonCode;
    private String reasonDetail;
    private Short status;
    private Short reviewAction;
    private String reviewNote;
    private Long reporterId;
    private String reporterDisplayName;
    private String reporterUsername;
    private String reporterAvatarUrl;
    private Long targetAuthorUserId;
    private String targetAuthorDisplayName;
    private String targetAuthorUsername;
    private String targetPoiName;
    private String targetContentPreview;
    private String currentTargetContent;
    private Boolean targetExists;
    private Long reviewedById;
    private String reviewedByDisplayName;
    private String reviewedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;

    public static AdminContentReportDetailResponse fromEntity(
            ContentReport entity,
            boolean targetExists,
            String currentTargetContent
    ) {
        return new AdminContentReportDetailResponse(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getRelatedShareId(),
                entity.getReasonCode(),
                entity.getReasonDetail(),
                entity.getStatus(),
                entity.getReviewAction(),
                entity.getReviewNote(),
                entity.getReporter().getId(),
                entity.getReporter().getDisplayName(),
                entity.getReporter().getUsername(),
                entity.getReporter().getAvatarUrl(),
                entity.getTargetAuthorUserId(),
                entity.getTargetAuthorDisplayName(),
                entity.getTargetAuthorUsername(),
                entity.getTargetPoiName(),
                entity.getTargetContentPreview(),
                currentTargetContent,
                targetExists,
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null,
                entity.getReviewedBy() != null ? entity.getReviewedBy().getDisplayName() : null,
                entity.getReviewedBy() != null ? entity.getReviewedBy().getUsername() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReviewedAt()
        );
    }
}
