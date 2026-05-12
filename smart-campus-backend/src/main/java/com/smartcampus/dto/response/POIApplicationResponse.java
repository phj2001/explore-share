package com.smartcampus.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record POIApplicationResponse(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String photoUrls,
        Short status,
        String reviewNote,
        Long createdPoiId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}
