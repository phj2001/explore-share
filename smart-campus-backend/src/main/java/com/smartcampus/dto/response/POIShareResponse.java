package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShare;
import com.smartcampus.security.UserRole;
import com.smartcampus.util.ImageThumbnailUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIShareResponse {

    private Long id;
    private Long poiId;
    private String content;
    private List<String> imageUrls;
    private List<String> imageThumbnailUrls;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private Long likeCount;
    private Boolean likedByCurrentUser;
    private Long replyCount;
    private List<POIShareReplyResponse> previewReplies;
    private Boolean canDelete;

    public static POIShareResponse fromEntity(
            POIShare share,
            Long currentUserId,
            Short currentUserRole,
            Long likeCount,
            boolean likedByCurrentUser,
            Long replyCount,
            List<POIShareReplyResponse> previewReplies
    ) {
        boolean isOwner = currentUserId != null && currentUserId.equals(share.getUser().getId());
        boolean isSuperAdmin = UserRole.SUPER_ADMIN.getCode() == (currentUserRole == null ? -1 : currentUserRole);
        String displayName = StringUtils.hasText(share.getUser().getDisplayName())
                ? share.getUser().getDisplayName()
                : share.getUser().getUsername();

        List<String> imageUrls = share.getImages().stream().map(image -> image.getImageUrl()).toList();

        return new POIShareResponse(
                share.getId(),
                share.getPoi().getId(),
                share.getContent(),
                imageUrls,
                imageUrls.stream().map(ImageThumbnailUtils::resolveThumbnailUrl).toList(),
                share.getUser().getId(),
                displayName,
                share.getUser().getUsername(),
                share.getUser().getAvatarUrl(),
                share.getCreatedAt(),
                likeCount,
                likedByCurrentUser,
                replyCount,
                previewReplies,
                isOwner || isSuperAdmin
        );
    }
}
