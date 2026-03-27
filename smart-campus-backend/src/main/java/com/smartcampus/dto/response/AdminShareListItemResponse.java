package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShare;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShareListItemResponse {

    private Long id;
    private Long poiId;
    private String poiName;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private String contentPreview;
    private Integer imageCount;
    private Long likeCount;
    private Long replyCount;
    private LocalDateTime createdAt;

    public static AdminShareListItemResponse fromEntity(POIShare share, Long likeCount, Long replyCount) {
        String displayName = StringUtils.hasText(share.getUser().getDisplayName())
                ? share.getUser().getDisplayName()
                : share.getUser().getUsername();

        return new AdminShareListItemResponse(
                share.getId(),
                share.getPoi().getId(),
                share.getPoi().getName(),
                share.getUser().getId(),
                displayName,
                share.getUser().getUsername(),
                share.getUser().getAvatarUrl(),
                summarizeContent(share.getContent(), 80),
                share.getImages() == null ? 0 : share.getImages().size(),
                likeCount,
                replyCount,
                share.getCreatedAt()
        );
    }

    private static String summarizeContent(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
