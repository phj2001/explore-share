package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiGalleryImageResponse {

    private Long imageId;

    private String imageUrl;

    private Long shareId;

    private Long userId;

    private String displayName;

    private String avatarUrl;

    private LocalDateTime createdAt;
}
