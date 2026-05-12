package com.smartcampus.service;

import com.smartcampus.dto.response.AchievementResponse;

import java.util.List;

public interface AchievementService {

    void checkAndUnlock(Long userId);

    List<AchievementResponse> getUserAchievements(Long userId);

    List<AchievementResponse> getAllDefinitions();
}
