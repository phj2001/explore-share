package com.smartcampus.dto.response;

import com.smartcampus.entity.RecommendedShare;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecommendedShareListItemResponse {

    private Long id;
    private Long shareId;
    private Long poiId;
    private String poiName;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private String contentPreview;
    private String coverImageUrl;
    private Integer imageCount;
    private Long likeCount;
    private Long replyCount;
    private Integer sortOrder;
    private String recommendationText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminRecommendedShareListItemResponse fromEntity(RecommendedShare recommendedShare, Long likeCount, Long replyCount) {
        String displayName = StringUtils.hasText(recommendedShare.getShare().getUser().getDisplayName())
                ? recommendedShare.getShare().getUser().getDisplayName()
                : recommendedShare.getShare().getUser().getUsername();

        String coverImageUrl = recommendedShare.getShare().getImages().isEmpty()
                ? null
                : recommendedShare.getShare().getImages().get(0).getImageUrl();

        return new AdminRecommendedShareListItemResponse(
                recommendedShare.getId(),
                recommendedShare.getShare().getId(),
                recommendedShare.getShare().getPoi().getId(),
                recommendedShare.getShare().getPoi().getName(),
                recommendedShare.getShare().getUser().getId(),
                displayName,
                recommendedShare.getShare().getUser().getUsername(),
                recommendedShare.getShare().getUser().getAvatarUrl(),
                summarizeContent(recommendedShare.getShare().getContent(), 80),
                coverImageUrl,
                recommendedShare.getShare().getImages().size(),
                likeCount,
                replyCount,
                recommendedShare.getSortOrder(),
                recommendedShare.getRecommendationText(),
                recommendedShare.getCreatedAt(),
                recommendedShare.getUpdatedAt()
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
