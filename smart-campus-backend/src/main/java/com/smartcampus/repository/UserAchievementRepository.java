package com.smartcampus.repository;

import com.smartcampus.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId);

    boolean existsByUserIdAndAchievementId(Long userId, String achievementId);

    Set<String> findAchievementIdByUserId(Long userId);
}
