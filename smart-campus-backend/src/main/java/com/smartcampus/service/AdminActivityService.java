package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminActivityDetailResponse;
import com.smartcampus.dto.response.AdminActivityListItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface AdminActivityService {

    PageResponse<AdminActivityListItemResponse> getActivities(
            String keyword,
            Short status,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    );

    AdminActivityDetailResponse getActivityDetail(Long activityId);

    AdminActivityDetailResponse createActivity(
            String title,
            String summary,
            String content,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Short status,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminActivityDetailResponse updateActivity(
            Long activityId,
            String title,
            String summary,
            String content,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Short status,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminActivityDetailResponse updatePublishStatus(Long activityId, Boolean published, Long operatorUserId);

    void deleteActivity(Long activityId, Long operatorUserId);
}
