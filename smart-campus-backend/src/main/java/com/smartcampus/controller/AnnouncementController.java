package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AnnouncementDetailResponse;
import com.smartcampus.dto.response.AnnouncementListItemResponse;
import com.smartcampus.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<Result<List<AnnouncementListItemResponse>>> getAnnouncements(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(announcementService.getPublishedAnnouncements(limit)));
    }

    @GetMapping("/{announcementId}")
    public ResponseEntity<Result<AnnouncementDetailResponse>> getAnnouncementDetail(@PathVariable Long announcementId) {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .body(Result.success(announcementService.getPublishedAnnouncementDetail(announcementId)));
    }
}
