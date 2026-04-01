package com.smartcampus.dto.response;

import com.smartcampus.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDetailResponse {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private Long poiId;
    private String poiName;
    private String poiCategory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime publishedAt;

    public static ActivityDetailResponse fromEntity(Activity entity) {
        return new ActivityDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCoverImageUrl(),
                entity.getPoi() != null ? entity.getPoi().getId() : null,
                entity.getPoi() != null ? entity.getPoi().getName() : null,
                entity.getPoi() != null ? entity.getPoi().getCategory() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getPublishedAt()
        );
    }
}
