package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AchievementResponse;
import com.smartcampus.entity.AchievementDefinition;
import com.smartcampus.entity.User;
import com.smartcampus.entity.UserAchievement;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AchievementDefinitionRepository;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserAchievementRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.repository.UserRouteRepository;
import com.smartcampus.security.ProfileVisibilityGuard;
import com.smartcampus.service.AchievementService;
import com.smartcampus.service.NotificationService;
import com.smartcampus.service.achievement.AchievementRules;
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
    private final ProfileVisibilityGuard profileVisibilityGuard;

    /** 用户 5 项行为指标快照，解锁判定与进度展示共用同一口径 */
    private record UserMetrics(long checkInCount, long shareCount, long likesCount, long routeCount, long categoryCount) {

        long get(AchievementRules.Metric metric) {
            return switch (metric) {
                case CHECK_IN -> checkInCount;
                case SHARE -> shareCount;
                case RECEIVED_LIKE -> likesCount;
                case DISTINCT_CATEGORY -> categoryCount;
                case ROUTE -> routeCount;
            };
        }
    }

    private UserMetrics loadMetrics(Long userId) {
        return new UserMetrics(
                poiCheckInRepository.countByUserId(userId),
                poiShareRepository.countByUserId(userId),
                poiShareLikeRepository.countReceivedLikesByUserId(userId),
                userRouteRepository.countByUserId(userId),
                poiCheckInRepository.countDistinctCategoriesByUserId(userId)
        );
    }

    @Override
    @Transactional
    public void checkAndUnlock(Long userId) {
        Set<String> unlocked = userAchievementRepository.findAchievementIdByUserId(userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        UserMetrics metrics = loadMetrics(userId);

        List<UserAchievement> newAchievements = new ArrayList<>();
        AchievementRules.all().forEach((achievementId, rule) -> {
            if (metrics.get(rule.metric()) >= rule.threshold() && !unlocked.contains(achievementId)) {
                UserAchievement ua = new UserAchievement();
                ua.setUser(user);
                ua.setAchievementId(achievementId);
                newAchievements.add(ua);
            }
        });

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
    public List<AchievementResponse> getUserAchievements(Long userId, Long viewerId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        profileVisibilityGuard.checkContentVisible(targetUser, viewerId);

        List<AchievementDefinition> definitions = achievementDefinitionRepository.findAllByOrderBySortOrderAsc();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
        Map<String, UserAchievement> unlockedMap = userAchievements.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity(), (a, b) -> a));

        UserMetrics metrics = loadMetrics(userId);

        return definitions.stream()
                .map(def -> {
                    UserAchievement ua = unlockedMap.get(def.getId());
                    // 未解锁且有规则的成就附带进度（当前值封顶于阈值）；已解锁/无规则均为 null
                    Long progressCurrent = null;
                    Long progressTarget = null;
                    if (ua == null) {
                        var rule = AchievementRules.resolve(def.getId());
                        if (rule.isPresent()) {
                            progressTarget = rule.get().threshold();
                            progressCurrent = Math.min(metrics.get(rule.get().metric()), rule.get().threshold());
                        }
                    }
                    return new AchievementResponse(
                            def.getId(),
                            def.getName(),
                            def.getDescription(),
                            def.getIconUrl(),
                            def.getCategory(),
                            def.getSortOrder() != null ? def.getSortOrder() : 0,
                            ua != null,
                            ua != null ? ua.getUnlockedAt() : null,
                            progressCurrent,
                            progressTarget
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
                        null,
                        null,
                        null
                ))
                .toList();
    }
}
