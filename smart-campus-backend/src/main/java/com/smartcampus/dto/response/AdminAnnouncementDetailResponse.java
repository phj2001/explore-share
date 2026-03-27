package com.smartcampus.dto.response;

import com.smartcampus.entity.Announcement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnnouncementDetailResponse {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private Short status;
    private Boolean pinned;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminAnnouncementDetailResponse fromEntity(Announcement entity) {
        return new AdminAnnouncementDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCoverImageUrl(),
                entity.getStatus(),
                entity.getPinned(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
