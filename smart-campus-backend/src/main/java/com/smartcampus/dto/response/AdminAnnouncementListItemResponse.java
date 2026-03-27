package com.smartcampus.dto.response;

import com.smartcampus.entity.Announcement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnnouncementListItemResponse {

    private Long id;
    private String title;
    private String summary;
    private String coverImageUrl;
    private Short status;
    private Boolean pinned;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public static AdminAnnouncementListItemResponse fromEntity(Announcement entity) {
        return new AdminAnnouncementListItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getCoverImageUrl(),
                entity.getStatus(),
                entity.getPinned(),
                entity.getPublishedAt(),
                entity.getUpdatedAt()
        );
    }
}
