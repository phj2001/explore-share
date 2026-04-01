package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.ActivityDetailResponse;
import com.smartcampus.dto.response.ActivityListItemResponse;
import com.smartcampus.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public Result<List<ActivityListItemResponse>> getActivities(@RequestParam(required = false) Integer limit) {
        return Result.success(activityService.getPublishedActivities(limit));
    }

    @GetMapping("/{activityId}")
    public Result<ActivityDetailResponse> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(activityService.getPublishedActivityDetail(activityId));
    }
}
