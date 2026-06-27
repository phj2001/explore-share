package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIFavoriteResponse;
import com.smartcampus.dto.response.POIFavoriteStatusResponse;

public interface POIFavoriteService {

    POIFavoriteStatusResponse getStatus(Long poiId, Long userId);

    POIFavoriteStatusResponse addFavorite(Long poiId, Long userId);

    POIFavoriteStatusResponse removeFavorite(Long poiId, Long userId);

    PageResponse<POIFavoriteResponse> getUserFavorites(Long userId, Integer page, Integer size);
}
