package com.smartcampus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class POIFavoriteStatusResponse {

    private boolean favorited;

    private long favoriteCount;
}
