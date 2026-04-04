package com.smartcampus.dto.response;

import com.smartcampus.entity.Announcement;
import com.smartcampus.util.ImageThumbnailUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementListItemResponse {

    private Long id;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String coverThumbnailUrl;
    private Boolean pinned;
    private LocalDateTime publishedAt;

    public static AnnouncementListItemResponse fromEntity(Announcement entity) {
        return new AnnouncementListItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getCoverImageUrl(),
                ImageThumbnailUtils.resolveThumbnailUrl(entity.getCoverImageUrl()),
                entity.getPinned(),
                entity.getPublishedAt()
        );
    }
}
