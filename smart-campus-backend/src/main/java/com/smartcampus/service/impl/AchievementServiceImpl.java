package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AchievementResponse;
import com.smartcampus.entity.AchievementDefinition;
import com.smartcampus.entity.User;
import com.smartcampus.entity.UserAchievement;
import com.smartcampus.repository.AchievementDefinitionRepository;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserAchievementRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.repository.UserRouteRepository;
import com.smartcampus.service.AchievementService;
import com.smartcampus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final POICheckInRepository poiCheckInRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final UserRouteRepository userRouteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void checkAndUnlock(Long userId) {
        Set<String> unlocked = userAchievementRepository.findAchievementIdByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        long checkInCount = poiCheckInRepository.countByUserId(userId);
        long shareCount = poiShareRepository.countByUserId(userId);
        long likesCount = poiShareLikeRepository.countReceivedLikesByUserId(userId);
        long routeCount = userRouteRepository.countByUserId(userId);
        long categoryCount = poiCheckInRepository.countDistinctCategoriesByUserId(userId);

        Map<String, Boolean> conditions = Map.of(
                "check_in_1", checkInCount >= 1,
                "check_in_10", checkInCount >= 10,
                "check_in_50", checkInCount >= 50,
                "check_in_100", checkInCount >= 100,
                "share_1", shareCount >= 1,
                "share_10", shareCount >= 10,
                "likes_10", likesCount >= 10,
                "likes_100", likesCount >= 100,
                "category_5", categoryCount >= 5,
                "route_1", routeCount >= 1
        );

        List<UserAchievement> newAchievements = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : conditions.entrySet()) {
            if (entry.getValue() && !unlocked.contains(entry.getKey())) {
                UserAchievement ua = new UserAchievement();
                ua.setUser(user);
                ua.setAchievementId(entry.getKey());
                newAchievements.add(ua);
            }
        }

        if (!newAchievements.isEmpty()) {
            userAchievementRepository.saveAll(newAchievements);
            for (UserAchievement ua : newAchievements) {
                log.info("用户 {} 解锁成就: {}", userId, ua.getAchievementId());
                try {
                    AchievementDefinition def = achievementDefinitionRepository.findById(ua.getAchievementId()).orElse(null);
                    String name = def != null ? def.getName() : ua.getAchievementId();
                    notificationService.sendNotification(userId, null, "ACHIEVEMENT",
                            "恭喜解锁成就: " + name, null, "ACHIEVEMENT", null);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getUserAchievements(Long userId) {
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findAllByOrderBySortOrderAsc();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
        Map<String, UserAchievement> unlockedMap = userAchievements.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity(), (a, b) -> a));

        return definitions.stream()
                .map(def -> {
                    UserAchievement ua = unlockedMap.get(def.getId());
                    return new AchievementResponse(
                            def.getId(),
                            def.getName(),
                            def.getDescription(),
                            def.getIconUrl(),
                            def.getCategory(),
                            def.getSortOrder() != null ? def.getSortOrder() : 0,
                            ua != null,
                            ua != null ? ua.getUnlockedAt() : null
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllDefinitions() {
        return achievementDefinitionRepository.findAllByOrderBySortOrderAsc().stream()
                .map(def -> new AchievementResponse(
                        def.getId(),
                        def.getName(),
                        def.getDescription(),
                        def.getIconUrl(),
                        def.getCategory(),
                        def.getSortOrder() != null ? def.getSortOrder() : 0,
                        false,
                        null
                ))
                .toList();
    }
}
