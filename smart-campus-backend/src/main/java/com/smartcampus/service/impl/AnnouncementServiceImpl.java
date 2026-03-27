package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AnnouncementDetailResponse;
import com.smartcampus.dto.response.AnnouncementListItemResponse;
import com.smartcampus.entity.Announcement;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AnnouncementRepository;
import com.smartcampus.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 12;

    private final AnnouncementRepository announcementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementListItemResponse> getPublishedAnnouncements(Integer limit) {
        int size = Math.min(Math.max(limit == null ? DEFAULT_LIMIT : limit, 1), MAX_LIMIT);
        return announcementRepository
                .findByStatusOrderByPinnedDescPublishedAtDescIdDesc(Announcement.STATUS_PUBLISHED, PageRequest.of(0, size))
                .stream()
                .map(AnnouncementListItemResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDetailResponse getPublishedAnnouncementDetail(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndStatus(announcementId, Announcement.STATUS_PUBLISHED)
                .orElseThrow(() -> new BusinessException(404, "公告不存在或尚未发布"));
        return AnnouncementDetailResponse.fromEntity(announcement);
    }
}
