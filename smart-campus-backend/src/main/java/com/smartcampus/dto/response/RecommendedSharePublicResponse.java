package com.smartcampus.dto.response;

import com.smartcampus.entity.RecommendedShare;
import com.smartcampus.util.ImageThumbnailUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedSharePublicResponse {

    private Long recommendationId;
    private Long shareId;
    private Long poiId;
    private String poiName;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private String contentPreview;
    private String coverImageUrl;
    private String coverThumbnailUrl;
    private Integer imageCount;
    private Long likeCount;
    private Long replyCount;
    private String recommendationText;

    public static RecommendedSharePublicResponse fromEntity(RecommendedShare recommendedShare, Long likeCount, Long replyCount) {
        String displayName = StringUtils.hasText(recommendedShare.getShare().getUser().getDisplayName())
                ? recommendedShare.getShare().getUser().getDisplayName()
                : recommendedShare.getShare().getUser().getUsername();

        String coverImageUrl = recommendedShare.getShare().getImages().isEmpty()
                ? null
                : recommendedShare.getShare().getImages().get(0).getImageUrl();

        return new RecommendedSharePublicResponse(
                recommendedShare.getId(),
                recommendedShare.getShare().getId(),
                recommendedShare.getShare().getPoi().getId(),
                recommendedShare.getShare().getPoi().getName(),
                displayName,
                recommendedShare.getShare().getUser().getUsername(),
                recommendedShare.getShare().getUser().getAvatarUrl(),
                summarizeContent(recommendedShare.getShare().getContent(), 120),
                coverImageUrl,
                ImageThumbnailUtils.resolveThumbnailUrl(coverImageUrl),
                recommendedShare.getShare().getImages().size(),
                likeCount,
                replyCount,
                recommendedShare.getRecommendationText()
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
