package com.smartcampus.dto.response;

import com.smartcampus.entity.Announcement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDetailResponse {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private Boolean pinned;
    private LocalDateTime publishedAt;

    public static AnnouncementDetailResponse fromEntity(Announcement entity) {
        return new AnnouncementDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCoverImageUrl(),
                entity.getPinned(),
                entity.getPublishedAt()
        );
    }
}
