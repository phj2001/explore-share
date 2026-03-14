package com.smartcampus.service.impl;

import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final POIRepository poiRepository;

    @Override
    public List<Object> planRoute(Double startLat, Double startLng, Double endLat, Double endLng) {
        // TODO: 实现路径规划逻辑
        // 这里可以集成路径规划算法或第三方地图API
        return Collections.emptyList();
    }

    @Override
    public List<Object> findNearbyPOIs(Double lat, Double lng, Double radius) {
        // TODO: 实现附近POI查询逻辑
        // 可以使用POIRepository的findWithinBounds方法
        return Collections.emptyList();
    }
}
