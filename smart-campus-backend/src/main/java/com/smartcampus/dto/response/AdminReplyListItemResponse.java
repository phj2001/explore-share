package com.smartcampus.dto.response;

import com.smartcampus.entity.POIShareReply;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReplyListItemResponse {

    private Long id;
    private Long shareId;
    private Long shareAuthorUserId;
    private String shareAuthorDisplayName;
    private String shareContentPreview;
    private Long authorUserId;
    private String authorDisplayName;
    private String authorUsername;
    private String authorAvatarUrl;
    private String content;
    private LocalDateTime createdAt;

    public static AdminReplyListItemResponse fromEntity(POIShareReply reply) {
        String replyAuthorDisplayName = StringUtils.hasText(reply.getUser().getDisplayName())
                ? reply.getUser().getDisplayName()
                : reply.getUser().getUsername();
        String shareAuthorDisplayName = StringUtils.hasText(reply.getShare().getUser().getDisplayName())
                ? reply.getShare().getUser().getDisplayName()
                : reply.getShare().getUser().getUsername();

        return new AdminReplyListItemResponse(
                reply.getId(),
                reply.getShare().getId(),
                reply.getShare().getUser().getId(),
                shareAuthorDisplayName,
                summarizeContent(reply.getShare().getContent(), 70),
                reply.getUser().getId(),
                replyAuthorDisplayName,
                reply.getUser().getUsername(),
                reply.getUser().getAvatarUrl(),
                reply.getContent(),
                reply.getCreatedAt()
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
