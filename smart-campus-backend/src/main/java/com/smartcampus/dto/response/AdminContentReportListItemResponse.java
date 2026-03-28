package com.smartcampus.dto.response;

import com.smartcampus.entity.ContentReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentReportListItemResponse {

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
    private String targetContentPreview;
    private String targetAuthorDisplayName;
    private String targetAuthorUsername;
    private String targetPoiName;
    private Boolean targetExists;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static AdminContentReportListItemResponse fromEntity(ContentReport entity, boolean targetExists) {
        return new AdminContentReportListItemResponse(
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
                entity.getTargetContentPreview(),
                entity.getTargetAuthorDisplayName(),
                entity.getTargetAuthorUsername(),
                entity.getTargetPoiName(),
                targetExists,
                entity.getCreatedAt(),
                entity.getReviewedAt()
        );
    }
}
