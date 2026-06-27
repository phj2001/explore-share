package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIReviewResponse {

    private Long id;

    private Long poiId;

    private String poiName;

    private Long authorId;

    private String authorUsername;

    private String authorDisplayName;

    private String authorAvatarUrl;

    private int rating;

    private String content;

    private boolean canDelete;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
