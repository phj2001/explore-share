package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIFavoriteResponse {

    private Long favoriteId;

    private Long poiId;

    private String poiName;

    private String category;

    private String description;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDateTime favoritedAt;
}
