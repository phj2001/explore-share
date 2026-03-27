package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShare;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShareDetailResponse {

    private Long id;
    private Long poiId;
    private String poiName;
    private String poiCategory;
    private String content;
    private List<String> imageUrls;
    private Integer imageCount;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long likeCount;
    private Long replyCount;
    private List<POIShareReplyResponse> replies;

    public static AdminShareDetailResponse fromEntity(
            POIShare share,
            Long likeCount,
            Long replyCount,
            List<POIShareReplyResponse> replies
    ) {
        String displayName = StringUtils.hasText(share.getUser().getDisplayName())
                ? share.getUser().getDisplayName()
                : share.getUser().getUsername();
        List<String> imageUrls = share.getImages().stream().map(image -> image.getImageUrl()).toList();

        return new AdminShareDetailResponse(
                share.getId(),
                share.getPoi().getId(),
                share.getPoi().getName(),
                share.getPoi().getCategory(),
                share.getContent(),
                imageUrls,
                imageUrls.size(),
                share.getUser().getId(),
                displayName,
                share.getUser().getUsername(),
                share.getUser().getAvatarUrl(),
                share.getCreatedAt(),
                share.getUpdatedAt(),
                likeCount,
                replyCount,
                replies
        );
    }
}
