package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemResponse {

    private String type;

    private Long id;

    private Long userId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private Long poiId;

    private String poiName;

    private String poiCategory;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String content;

    private List<String> imageUrls;

    private long likeCount;

    private long replyCount;

    private LocalDateTime createdAt;
}
