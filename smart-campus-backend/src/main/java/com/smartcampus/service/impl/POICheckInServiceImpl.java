package com.smartcampus.service.impl;

import com.smartcampus.annotation.OperationLog;
import com.smartcampus.dto.response.POICheckInStatusResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POICheckIn;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.AchievementService;
import com.smartcampus.service.POICheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class POICheckInServiceImpl implements POICheckInService {

    private final POICheckInRepository poiCheckInRepository;
    private final POIRepository poiRepository;
    private final UserRepository userRepository;
    private final AchievementService achievementService;

    @Override
    @Transactional(readOnly = true)
    public POICheckInStatusResponse getStatus(Long poiId, Long userId) {
        POI poi = getRequiredPoi(poiId);
        long checkInCount = poiCheckInRepository.countByPoiId(poi.getId());
        boolean checkedIn = userId != null && poiCheckInRepository.existsByPoiIdAndUserId(poi.getId(), userId);
        return new POICheckInStatusResponse(checkedIn, checkInCount);
    }

    @Override
    @Transactional
    @OperationLog(module = "地点打卡", action = "打卡", targetType = "地点", targetIdSpel = "#poiId")
    public POICheckInStatusResponse checkIn(Long poiId, Long userId) {
        POI poi = getRequiredPoi(poiId);
        User user = getRequiredUser(userId);

        if (!poiCheckInRepository.existsByPoiIdAndUserId(poi.getId(), user.getId())) {
            POICheckIn checkIn = new POICheckIn();
            checkIn.setPoi(poi);
            checkIn.setUser(user);
            poiCheckInRepository.save(checkIn);
        }

        try {
            achievementService.checkAndUnlock(userId);
        } catch (Exception ignored) {
        }

        return buildStatus(poi.getId(), user.getId(), true);
    }

    @Override
    @Transactional
    public POICheckInStatusResponse cancelCheckIn(Long poiId, Long userId) {
        POI poi = getRequiredPoi(poiId);
        getRequiredUser(userId);

        poiCheckInRepository.findByPoiIdAndUserId(poi.getId(), userId)
                .ifPresent(poiCheckInRepository::delete);

        return buildStatus(poi.getId(), userId, false);
    }

    private POICheckInStatusResponse buildStatus(Long poiId, Long userId, boolean checkedIn) {
        long checkInCount = poiCheckInRepository.countByPoiId(poiId);
        return new POICheckInStatusResponse(checkedIn, checkInCount);
    }

    private POI getRequiredPoi(Long poiId) {
        return poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(404, "POI不存在"));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
