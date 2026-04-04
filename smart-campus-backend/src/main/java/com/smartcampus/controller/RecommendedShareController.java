package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.RecommendedSharePublicResponse;
import com.smartcampus.service.RecommendedShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/recommended-shares")
@RequiredArgsConstructor
public class RecommendedShareController {
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final RecommendedShareService recommendedShareService;

    @GetMapping
    public ResponseEntity<Result<List<RecommendedSharePublicResponse>>> getRecommendedShares(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(recommendedShareService.getPublicRecommendedShares(limit)));
    }
}
