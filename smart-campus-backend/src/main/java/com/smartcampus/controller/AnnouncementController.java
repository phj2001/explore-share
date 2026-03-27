package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AnnouncementDetailResponse;
import com.smartcampus.dto.response.AnnouncementListItemResponse;
import com.smartcampus.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public Result<List<AnnouncementListItemResponse>> getAnnouncements(
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(announcementService.getPublishedAnnouncements(limit));
    }

    @GetMapping("/{announcementId}")
    public Result<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable Long announcementId) {
        return Result.success(announcementService.getPublishedAnnouncementDetail(announcementId));
    }
}
