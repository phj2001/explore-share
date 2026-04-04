package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.ActivityDetailResponse;
import com.smartcampus.dto.response.ActivityListItemResponse;
import com.smartcampus.service.ActivityService;
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
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Result<List<ActivityListItemResponse>>> getActivities(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(activityService.getPublishedActivities(limit)));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<Result<ActivityDetailResponse>> getActivityDetail(@PathVariable Long activityId) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(activityService.getPublishedActivityDetail(activityId)));
    }
}
