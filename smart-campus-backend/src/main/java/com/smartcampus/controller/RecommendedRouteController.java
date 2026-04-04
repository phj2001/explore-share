package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.RecommendedRouteDetailResponse;
import com.smartcampus.dto.response.RecommendedRouteListItemResponse;
import com.smartcampus.service.RecommendedRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/recommended-routes")
@RequiredArgsConstructor
public class RecommendedRouteController {
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final RecommendedRouteService recommendedRouteService;

    @GetMapping
    public ResponseEntity<Result<List<RecommendedRouteListItemResponse>>> getPublishedRoutes(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(recommendedRouteService.getPublishedRoutes(limit)));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<Result<RecommendedRouteDetailResponse>> getPublishedRouteDetail(@PathVariable Long routeId) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(recommendedRouteService.getPublishedRouteDetail(routeId)));
    }
}
