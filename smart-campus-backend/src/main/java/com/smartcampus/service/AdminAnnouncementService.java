package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminAnnouncementDetailResponse;
import com.smartcampus.dto.response.AdminAnnouncementListItemResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AdminAnnouncementService {

    PageResponse<AdminAnnouncementListItemResponse> getAnnouncements(String keyword, Short status, Boolean pinned, Integer page, Integer size);

    AdminAnnouncementDetailResponse getAnnouncementDetail(Long announcementId);

    AdminAnnouncementDetailResponse createAnnouncement(
            String title,
            String summary,
            String content,
            Boolean pinned,
            Short status,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminAnnouncementDetailResponse updateAnnouncement(
            Long announcementId,
            String title,
            String summary,
            String content,
            Boolean pinned,
            Short status,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    );

    AdminAnnouncementDetailResponse updatePublishStatus(Long announcementId, Boolean published, Long operatorUserId);

    AdminAnnouncementDetailResponse updatePinnedStatus(Long announcementId, Boolean pinned, Long operatorUserId);

    void deleteAnnouncement(Long announcementId, Long operatorUserId);
}
