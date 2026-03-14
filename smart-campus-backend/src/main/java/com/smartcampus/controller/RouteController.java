package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * 规划两点之间的路径
     * POST /api/routes/plan
     */
    @PostMapping("/plan")
    public Result<List<Object>> planRoute(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng) {
        List<Object> route = routeService.planRoute(startLat, startLng, endLat, endLng);
        return Result.success(route);
    }

    /**
     * 查询附近的POI
     * GET /api/routes/nearby?lat=xxx&lng=xxx&radius=xxx
     */
    @GetMapping("/nearby")
    public Result<List<Object>> findNearbyPOIs(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam Double radius) {
        List<Object> nearbyPOIs = routeService.findNearbyPOIs(lat, lng, radius);
        return Result.success(nearbyPOIs);
    }
}
