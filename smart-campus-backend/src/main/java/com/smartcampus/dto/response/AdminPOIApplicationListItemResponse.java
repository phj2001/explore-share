package com.smartcampus.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPOIApplicationListItemResponse(
        Long id,
        Long applicantId,
        String applicantName,
        String applicantAvatarUrl,
        String name,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String description,
        Short status,
        String reviewedByName,
        String reviewNote,
        Long createdPoiId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {}
