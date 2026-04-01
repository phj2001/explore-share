package com.smartcampus.service;

import com.smartcampus.dto.response.ActivityDetailResponse;
import com.smartcampus.dto.response.ActivityListItemResponse;

import java.util.List;

public interface ActivityService {

    List<ActivityListItemResponse> getPublishedActivities(Integer limit);

    ActivityDetailResponse getPublishedActivityDetail(Long activityId);
}
