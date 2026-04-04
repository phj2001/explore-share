package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShare;
import com.smartcampus.util.ImageThumbnailUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecommendedShareCandidateResponse {

    private Long shareId;
    private Long poiId;
    private String poiName;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private String contentPreview;
    private String coverImageUrl;
    private String coverThumbnailUrl;
    private Integer imageCount;
    private Long likeCount;
    private Long replyCount;
    private Boolean recommended;
    private LocalDateTime createdAt;

    public static AdminRecommendedShareCandidateResponse fromEntity(
            POIShare share,
            Long likeCount,
            Long replyCount,
            boolean recommended
    ) {
        String displayName = StringUtils.hasText(share.getUser().getDisplayName())
                ? share.getUser().getDisplayName()
                : share.getUser().getUsername();

        String coverImageUrl = share.getImages().isEmpty() ? null : share.getImages().get(0).getImageUrl();

        return new AdminRecommendedShareCandidateResponse(
                share.getId(),
                share.getPoi().getId(),
                share.getPoi().getName(),
                share.getUser().getId(),
                displayName,
                share.getUser().getUsername(),
                share.getUser().getAvatarUrl(),
                summarizeContent(share.getContent(), 80),
                coverImageUrl,
                ImageThumbnailUtils.resolveThumbnailUrl(coverImageUrl),
                share.getImages().size(),
                likeCount,
                replyCount,
                recommended,
                share.getCreatedAt()
        );
    }

    private static String summarizeContent(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "仅图片分享";
        }
        String normalized = content.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
