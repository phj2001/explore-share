package com.smartcampus.dto.response;

import com.smartcampus.entity.Activity;
import com.smartcampus.util.ImageThumbnailUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivityListItemResponse {

    private Long id;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String coverThumbnailUrl;
    private Long poiId;
    private String poiName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Short status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public static AdminActivityListItemResponse fromEntity(Activity entity) {
        return new AdminActivityListItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getCoverImageUrl(),
                ImageThumbnailUtils.resolveThumbnailUrl(entity.getCoverImageUrl()),
                entity.getPoi() != null ? entity.getPoi().getId() : null,
                entity.getPoi() != null ? entity.getPoi().getName() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getUpdatedAt()
        );
    }
}
