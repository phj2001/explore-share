package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AnnouncementDetailResponse;
import com.smartcampus.dto.response.AnnouncementListItemResponse;
import com.smartcampus.entity.Announcement;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AnnouncementRepository;
import com.smartcampus.service.AnnouncementService;
import com.smartcampus.service.SystemConfigService;
import com.smartcampus.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int    MAX_LIMIT               = 12;
    private static final String ANNOUNCE_CACHE_PREFIX   = "announcements:published:";
    private static final long   ANNOUNCE_CACHE_TTL      = 300L; // 5 分钟

    private final AnnouncementRepository announcementRepository;
    private final SystemConfigService systemConfigService;
    private final RedisUtils redisUtils;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementListItemResponse> getPublishedAnnouncements(Integer limit) {
        int configuredDefault = systemConfigService.getIntValue(SystemConfigService.HOME_ANNOUNCEMENT_LIMIT);
        int size = Math.min(Math.max(limit == null ? configuredDefault : limit, 1), MAX_LIMIT);

        String cacheKey = ANNOUNCE_CACHE_PREFIX + size;
        List<AnnouncementListItemResponse> cached = redisUtils.getList(cacheKey, AnnouncementListItemResponse.class);
        if (cached != null) {
            return cached;
        }

        List<AnnouncementListItemResponse> result = announcementRepository
                .findByStatusOrderByPinnedDescPublishedAtDescIdDesc(Announcement.STATUS_PUBLISHED, PageRequest.of(0, size))
                .stream()
                .map(AnnouncementListItemResponse::fromEntity)
                .toList();

        redisUtils.setObject(cacheKey, result, ANNOUNCE_CACHE_TTL, TimeUnit.SECONDS);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDetailResponse getPublishedAnnouncementDetail(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndStatus(announcementId, Announcement.STATUS_PUBLISHED)
                .orElseThrow(() -> new BusinessException(404, "公告不存在或尚未发布"));
        return AnnouncementDetailResponse.fromEntity(announcement);
    }
}
