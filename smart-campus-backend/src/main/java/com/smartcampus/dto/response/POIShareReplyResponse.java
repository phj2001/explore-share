package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShareReply;
import com.smartcampus.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIShareReplyResponse {

    private Long id;
    private Long shareId;
    private String content;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private LocalDateTime createdAt;
    private Boolean canDelete;

    public static POIShareReplyResponse fromEntity(POIShareReply reply, Long currentUserId, Short currentUserRole) {
        boolean isOwner = currentUserId != null && currentUserId.equals(reply.getUser().getId());
        boolean isSuperAdmin = UserRole.SUPER_ADMIN.getCode() == (currentUserRole == null ? -1 : currentUserRole);
        String displayName = StringUtils.hasText(reply.getUser().getDisplayName())
                ? reply.getUser().getDisplayName()
                : reply.getUser().getUsername();

        return new POIShareReplyResponse(
                reply.getId(),
                reply.getShare().getId(),
                reply.getContent(),
                reply.getUser().getId(),
                displayName,
                reply.getUser().getUsername(),
                reply.getUser().getAvatarUrl(),
                reply.getCreatedAt(),
                isOwner || isSuperAdmin
        );
    }
}
