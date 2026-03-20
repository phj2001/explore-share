package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.RoutePlanResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.enums.RouteMode;
import com.smartcampus.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/plan")
    public Result<RoutePlanResponse> planRoute(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng,
            @RequestParam(defaultValue = "walking") String mode) {
        RouteMode routeMode = RouteMode.fromValue(mode);
        return Result.success(routeService.planRoute(startLat, startLng, endLat, endLng, routeMode));
    }

    @GetMapping("/nearby")
    public Result<List<POI>> findNearbyPOIs(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam Double radius) {
        return Result.success(routeService.findNearbyPOIs(lat, lng, radius));
    }
}
