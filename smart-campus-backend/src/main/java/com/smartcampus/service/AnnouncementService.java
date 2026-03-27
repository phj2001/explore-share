package com.smartcampus.service;

import com.smartcampus.dto.response.AnnouncementDetailResponse;
import com.smartcampus.dto.response.AnnouncementListItemResponse;

import java.util.List;

public interface AnnouncementService {

    List<AnnouncementListItemResponse> getPublishedAnnouncements(Integer limit);

    AnnouncementDetailResponse getPublishedAnnouncementDetail(Long announcementId);
}
