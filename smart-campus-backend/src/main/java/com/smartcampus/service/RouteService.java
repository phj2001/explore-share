package com.smartcampus.service;

import com.smartcampus.dto.response.RoutePlanResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.enums.RouteMode;

import java.util.List;

public interface RouteService {

    RoutePlanResponse planRoute(Double startLat,
                                Double startLng,
                                Double endLat,
                                Double endLng,
                                RouteMode mode);

    List<POI> findNearbyPOIs(Double lat, Double lng, Double radiusMeters);
}
