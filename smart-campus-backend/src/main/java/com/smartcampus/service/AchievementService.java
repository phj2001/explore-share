package com.smartcampus.service;

import com.smartcampus.dto.response.AchievementResponse;

import java.util.List;

public interface AchievementService {

    void checkAndUnlock(Long userId);

    /** viewerId 为查看者（游客为 null）；不可见时抛 403 */
    List<AchievementResponse> getUserAchievements(Long userId, Long viewerId);

    List<AchievementResponse> getAllDefinitions();
}
