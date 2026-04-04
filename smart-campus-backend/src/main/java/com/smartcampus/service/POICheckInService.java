package com.smartcampus.service;

import com.smartcampus.dto.response.POICheckInStatusResponse;

public interface POICheckInService {

    POICheckInStatusResponse getStatus(Long poiId, Long userId);

    POICheckInStatusResponse checkIn(Long poiId, Long userId);

    POICheckInStatusResponse cancelCheckIn(Long poiId, Long userId);
}
