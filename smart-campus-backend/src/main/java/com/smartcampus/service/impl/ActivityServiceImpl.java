package com.smartcampus.service.impl;

import com.smartcampus.dto.response.ActivityDetailResponse;
import com.smartcampus.dto.response.ActivityListItemResponse;
import com.smartcampus.entity.Activity;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.ActivityRepository;
import com.smartcampus.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 12;

    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityListItemResponse> getPublishedActivities(Integer limit) {
        int size = Math.min(Math.max(limit == null ? DEFAULT_LIMIT : limit, 1), MAX_LIMIT);
        LocalDateTime now = LocalDateTime.now();

        return activityRepository.findByStatus(Activity.STATUS_PUBLISHED).stream()
                .sorted(buildComparator(now))
                .limit(size)
                .map(ActivityListItemResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityDetailResponse getPublishedActivityDetail(Long activityId) {
        Activity activity = activityRepository.findByIdAndStatus(activityId, Activity.STATUS_PUBLISHED)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));
        return ActivityDetailResponse.fromEntity(activity);
    }

    private Comparator<Activity> buildComparator(LocalDateTime now) {
        return Comparator
                .comparingInt((Activity item) -> resolveDisplayRank(item, now))
                .thenComparing(Activity::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(Activity::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Activity::getId, Comparator.reverseOrder());
    }

    private int resolveDisplayRank(Activity item, LocalDateTime now) {
        if ((item.getStartTime().isEqual(now) || item.getStartTime().isBefore(now))
                && (item.getEndTime().isEqual(now) || item.getEndTime().isAfter(now))) {
            return 0;
        }
        if (item.getStartTime().isAfter(now)) {
            return 1;
        }
        return 2;
    }
}
