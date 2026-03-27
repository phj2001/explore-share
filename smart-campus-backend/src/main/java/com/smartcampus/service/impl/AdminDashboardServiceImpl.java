package com.smartcampus.service.impl;

import com.smartcampus.dto.response.AdminOverviewResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POIShare;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final POIRepository poiRepository;
    private final UserRepository userRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIShareLikeRepository poiShareLikeRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview(Integer days) {
        int rangeDays = normalizeRangeDays(days);
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime rangeStart = today.minusDays(rangeDays - 1L).atStartOfDay();

        AdminOverviewResponse.OverviewSummary summary = new AdminOverviewResponse.OverviewSummary(
                poiRepository.count(),
                userRepository.count(),
                poiShareRepository.count(),
                poiShareReplyRepository.count(),
                poiShareLikeRepository.count(),
                poiShareRepository.countByCreatedAtGreaterThanEqual(startOfToday)
        );

        List<POIShare> rangeShares = poiShareRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(rangeStart);

        return new AdminOverviewResponse(
                rangeDays,
                summary,
                buildTrend(rangeStart.toLocalDate(), today, rangeShares),
                buildHotPois(rangeStart),
                buildRecentShares()
        );
    }

    private int normalizeRangeDays(Integer days) {
        return days != null && days == 30 ? 30 : 7;
    }

    private List<AdminOverviewResponse.OverviewTrendPoint> buildTrend(LocalDate startDate, LocalDate endDate, List<POIShare> rangeShares) {
        Map<LocalDate, Long> countByDate = new HashMap<>();
        for (POIShare share : rangeShares) {
            LocalDate date = share.getCreatedAt().toLocalDate();
            countByDate.put(date, countByDate.getOrDefault(date, 0L) + 1L);
        }

        List<AdminOverviewResponse.OverviewTrendPoint> trend = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            trend.add(new AdminOverviewResponse.OverviewTrendPoint(
                    cursor.format(DATE_FORMATTER),
                    countByDate.getOrDefault(cursor, 0L)
            ));
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private List<AdminOverviewResponse.OverviewHotPoi> buildHotPois(LocalDateTime rangeStart) {
        Map<Long, Long> shareCountMap = toCountMap(poiShareRepository.countGroupedByPoiIdsSince(rangeStart));
        Map<Long, Long> replyCountMap = toCountMap(poiShareReplyRepository.countGroupedByPoiIdsSince(rangeStart));

        Set<Long> poiIds = shareCountMap.keySet();
        if (poiIds.isEmpty()) {
            return List.of();
        }

        Map<Long, POI> poiMap = poiRepository.findAllById(poiIds).stream()
                .collect(Collectors.toMap(POI::getId, poi -> poi));

        return poiIds.stream()
                .map(poiId -> {
                    POI poi = poiMap.get(poiId);
                    if (poi == null) {
                        return null;
                    }
                    return new AdminOverviewResponse.OverviewHotPoi(
                            poi.getId(),
                            poi.getName(),
                            poi.getCategory(),
                            shareCountMap.getOrDefault(poiId, 0L),
                            replyCountMap.getOrDefault(poiId, 0L)
                    );
                })
                .filter(item -> item != null)
                .sorted(Comparator
                        .comparing(AdminOverviewResponse.OverviewHotPoi::getShareCount, Comparator.reverseOrder())
                        .thenComparing(AdminOverviewResponse.OverviewHotPoi::getReplyCount, Comparator.reverseOrder())
                        .thenComparing(AdminOverviewResponse.OverviewHotPoi::getPoiId))
                .limit(10)
                .toList();
    }

    private List<AdminOverviewResponse.OverviewRecentShare> buildRecentShares() {
        List<POIShare> recentShares = poiShareRepository.findTop5ByOrderByCreatedAtDesc();
        List<Long> shareIds = recentShares.stream().map(POIShare::getId).toList();
        Map<Long, Long> shareLikeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> shareReplyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        return recentShares.stream()
                .map(share -> new AdminOverviewResponse.OverviewRecentShare(
                        share.getId(),
                        share.getPoi().getId(),
                        share.getPoi().getName(),
                        resolveDisplayName(share),
                        share.getUser().getUsername(),
                        share.getUser().getAvatarUrl(),
                        buildContentPreview(share.getContent()),
                        (long) share.getImages().size(),
                        shareLikeCountMap.getOrDefault(share.getId(), 0L),
                        shareReplyCountMap.getOrDefault(share.getId(), 0L),
                        share.getCreatedAt().format(TIME_FORMATTER)
                ))
                .toList();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            Number key = (Number) row[0];
            Number value = (Number) row[1];
            result.put(key.longValue(), value.longValue());
        }
        return result;
    }

    private String resolveDisplayName(POIShare share) {
        if (StringUtils.hasText(share.getUser().getDisplayName())) {
            return share.getUser().getDisplayName();
        }
        return share.getUser().getUsername();
    }

    private String buildContentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "图片分享";
        }
        String normalized = content.trim();
        if (normalized.length() <= 48) {
            return normalized;
        }
        return normalized.substring(0, 48) + "...";
    }
}
