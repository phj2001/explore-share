package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminRecommendedRouteDetailResponse;
import com.smartcampus.dto.response.AdminRecommendedRouteListItemResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.RecommendedRoute;
import com.smartcampus.entity.RecommendedRouteWaypoint;
import com.smartcampus.enums.RouteMode;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.RecommendedRouteRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.AdminRecommendedRouteService;
import com.smartcampus.service.storage.StorageCategory;
import com.smartcampus.service.storage.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRecommendedRouteServiceImpl implements AdminRecommendedRouteService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 220;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;
    private static final int MAX_RECOMMENDATION_TEXT_LENGTH = 100;
    private static final int MIN_WAYPOINT_COUNT = 2;
    private static final int MAX_WAYPOINT_COUNT = 10;
    private static final long MAX_COVER_SIZE = 5L * 1024 * 1024;
    private static final String IMAGE_URL_PREFIX = "/uploads/routes/";
    private static final int COVER_THUMB_MAX_WIDTH = 480;
    private static final int COVER_THUMB_MAX_HEIGHT = 320;

    private final RecommendedRouteRepository recommendedRouteRepository;
    private final POIRepository poiRepository;
    private final AdminOperationLogService adminOperationLogService;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRecommendedRouteListItemResponse> getRoutes(
            String keyword,
            Short status,
            String defaultMode,
            Integer page,
            Integer size
    ) {
        validateStatusIfPresent(status);
        String normalizedMode = normalizeDefaultMode(defaultMode, false);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(
                        Sort.Order.asc("sortOrder"),
                        Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<RecommendedRoute> result = recommendedRouteRepository.findAll(buildSpecification(keyword, status, normalizedMode), pageable);
        List<AdminRecommendedRouteListItemResponse> records = result.getContent().stream()
                .map(route -> AdminRecommendedRouteListItemResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode())))
                .toList();
        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRecommendedRouteDetailResponse getRouteDetail(Long routeId) {
        RecommendedRoute route = getRequiredRoute(routeId);
        return AdminRecommendedRouteDetailResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode()));
    }

    @Override
    @Transactional
    public AdminRecommendedRouteDetailResponse createRoute(
            String title,
            String summary,
            String description,
            String recommendationText,
            Integer sortOrder,
            String defaultMode,
            Short status,
            List<Long> poiIds,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        RecommendedRoute route = new RecommendedRoute();
        applyRoutePayload(route, title, summary, description, recommendationText, sortOrder, defaultMode, status, poiIds);

        String savedImageUrl = null;
        try {
            savedImageUrl = saveImageIfPresent(coverImage);
            route.setCoverImageUrl(savedImageUrl);
            route = recommendedRouteRepository.save(route);
            adminOperationLogService.record(
                    operatorUserId,
                    "路线管理",
                    "创建推荐路线",
                    "推荐路线",
                    route.getId(),
                    "创建推荐路线《" + route.getTitle() + "》"
            );
            return AdminRecommendedRouteDetailResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode()));
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AdminRecommendedRouteDetailResponse updateRoute(
            Long routeId,
            String title,
            String summary,
            String description,
            String recommendationText,
            Integer sortOrder,
            String defaultMode,
            Short status,
            List<Long> poiIds,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        RecommendedRoute route = getRequiredRoute(routeId);
        String originalCoverImageUrl = route.getCoverImageUrl();
        String savedImageUrl = null;

        applyRoutePayload(route, title, summary, description, recommendationText, sortOrder, defaultMode, status, poiIds);

        try {
            if (Boolean.TRUE.equals(removeCoverImage)) {
                route.setCoverImageUrl(null);
            }
            if (coverImage != null && !coverImage.isEmpty()) {
                savedImageUrl = saveImageIfPresent(coverImage);
                route.setCoverImageUrl(savedImageUrl);
            }
            route = recommendedRouteRepository.save(route);

            if ((Boolean.TRUE.equals(removeCoverImage) || savedImageUrl != null) && !savedImageUrlEqualsOriginal(savedImageUrl, originalCoverImageUrl)) {
                deleteImageQuietly(originalCoverImageUrl);
            }

            adminOperationLogService.record(
                    operatorUserId,
                    "路线管理",
                    "更新推荐路线",
                    "推荐路线",
                    route.getId(),
                    "更新推荐路线《" + route.getTitle() + "》"
            );
            return AdminRecommendedRouteDetailResponse.fromEntity(route, resolveModeLabel(route.getDefaultMode()));
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            route.setCoverImageUrl(originalCoverImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AdminRecommendedRouteDetailResponse updatePublishStatus(Long routeId, Boolean published, Long operatorUserId) {
        if (published == null) {
            throw new BusinessException(400, "发布状态不能为空");
        }

        RecommendedRoute route = getRequiredRoute(routeId);
        if (published) {
            route.setStatus(RecommendedRoute.STATUS_PUBLISHED);
            route.setPublishedAt(LocalDateTime.now());
        } else {
            route.setStatus(RecommendedRoute.STATUS_DRAFT);
        }

        RecommendedRoute savedRoute = recommendedRouteRepository.save(route);
        adminOperationLogService.record(
                operatorUserId,
                "路线管理",
                published ? "发布推荐路线" : "取消发布推荐路线",
                "推荐路线",
                savedRoute.getId(),
                (published ? "发布推荐路线《" : "取消发布推荐路线《") + savedRoute.getTitle() + "》"
        );
        return AdminRecommendedRouteDetailResponse.fromEntity(savedRoute, resolveModeLabel(savedRoute.getDefaultMode()));
    }

    @Override
    @Transactional
    public void deleteRoute(Long routeId, Long operatorUserId) {
        RecommendedRoute route = getRequiredRoute(routeId);
        Long targetId = route.getId();
        String title = route.getTitle();
        String coverImageUrl = route.getCoverImageUrl();
        recommendedRouteRepository.delete(route);
        deleteImageQuietly(coverImageUrl);
        adminOperationLogService.record(
                operatorUserId,
                "路线管理",
                "删除推荐路线",
                "推荐路线",
                targetId,
                "删除推荐路线《" + title + "》"
        );
    }

    private Specification<RecommendedRoute> buildSpecification(String keyword, Short status, String defaultMode) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                var waypointJoin = root.join("waypoints", JoinType.LEFT);
                var poiJoin = waypointJoin.join("poi", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("recommendationText"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(poiJoin.get("name"), "")), normalizedKeyword)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(defaultMode)) {
                predicates.add(criteriaBuilder.equal(root.get("defaultMode"), defaultMode));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private RecommendedRoute getRequiredRoute(Long routeId) {
        return recommendedRouteRepository.findById(routeId)
                .orElseThrow(() -> new BusinessException(404, "推荐路线不存在"));
    }

    private void applyRoutePayload(
            RecommendedRoute route,
            String title,
            String summary,
            String description,
            String recommendationText,
            Integer sortOrder,
            String defaultMode,
            Short status,
            List<Long> poiIds
    ) {
        route.setTitle(normalizeRequiredText(title, "路线标题不能为空", MAX_TITLE_LENGTH, "路线标题不能超过120个字符"));
        route.setSummary(normalizeRequiredText(summary, "路线摘要不能为空", MAX_SUMMARY_LENGTH, "路线摘要不能超过220个字符"));
        route.setDescription(normalizeRequiredText(description, "路线说明不能为空", MAX_DESCRIPTION_LENGTH, "路线说明不能超过10000个字符"));
        route.setRecommendationText(normalizeOptionalText(recommendationText, MAX_RECOMMENDATION_TEXT_LENGTH, "推荐语不能超过100个字符"));
        route.setSortOrder(normalizeSortOrder(sortOrder));
        route.setDefaultMode(normalizeDefaultMode(defaultMode, true));
        route.setStatus(normalizeStatus(status));

        List<POI> orderedPois = resolvePoisInOrder(poiIds);
        rebuildWaypoints(route, orderedPois);

        if (route.getStatus() == RecommendedRoute.STATUS_PUBLISHED) {
            route.setPublishedAt(LocalDateTime.now());
        }
    }

    private List<POI> resolvePoisInOrder(List<Long> poiIds) {
        if (poiIds == null || poiIds.isEmpty()) {
            throw new BusinessException(400, "路线至少需要选择两个地点");
        }

        List<Long> normalizedPoiIds = poiIds.stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (normalizedPoiIds.size() < MIN_WAYPOINT_COUNT) {
            throw new BusinessException(400, "路线至少需要选择两个地点");
        }
        if (normalizedPoiIds.size() > MAX_WAYPOINT_COUNT) {
            throw new BusinessException(400, "单条路线最多支持10个地点");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(normalizedPoiIds);
        if (uniqueIds.size() != normalizedPoiIds.size()) {
            throw new BusinessException(400, "同一条路线中不能重复添加相同地点");
        }

        Map<Long, POI> poiMap = poiRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(POI::getId, Function.identity()));
        List<POI> orderedPois = new ArrayList<>(uniqueIds.size());
        for (Long poiId : uniqueIds) {
            POI poi = poiMap.get(poiId);
            if (poi == null) {
                throw new BusinessException(400, "路线中包含不存在的地点");
            }
            orderedPois.add(poi);
        }
        return orderedPois;
    }

    private void rebuildWaypoints(RecommendedRoute route, List<POI> orderedPois) {
        route.getWaypoints().clear();
        for (int index = 0; index < orderedPois.size(); index++) {
            RecommendedRouteWaypoint waypoint = new RecommendedRouteWaypoint();
            waypoint.setRoute(route);
            waypoint.setPoi(orderedPois.get(index));
            waypoint.setSortOrder(index + 1);
            route.getWaypoints().add(waypoint);
        }
    }

    private String normalizeRequiredText(String value, String emptyMessage, int maxLength, String lengthMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, emptyMessage);
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, emptyMessage);
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(400, lengthMessage);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength, String lengthMessage) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(400, lengthMessage);
        }
        return normalized;
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 1;
        }
        if (sortOrder < 1 || sortOrder > 9999) {
            throw new BusinessException(400, "路线排序值必须在 1 到 9999 之间");
        }
        return sortOrder;
    }

    private Short normalizeStatus(Short status) {
        if (status == null) {
            return RecommendedRoute.STATUS_DRAFT;
        }
        validateStatusIfPresent(status);
        return status;
    }

    private void validateStatusIfPresent(Short status) {
        if (status == null) {
            return;
        }
        if (status != RecommendedRoute.STATUS_DRAFT && status != RecommendedRoute.STATUS_PUBLISHED) {
            throw new BusinessException(400, "不支持的路线状态");
        }
    }

    private String normalizeDefaultMode(String defaultMode, boolean fallbackToWalking) {
        if (!StringUtils.hasText(defaultMode)) {
            return fallbackToWalking ? RouteMode.WALKING.getValue() : null;
        }
        try {
            return RouteMode.fromValue(defaultMode).getValue();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "不支持的路线规划模式");
        }
    }

    private String resolveModeLabel(String mode) {
        try {
            return RouteMode.fromValue(mode).getLabel();
        } catch (IllegalArgumentException ex) {
            return mode;
        }
    }

    private String saveImageIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(400, "路线封面读取失败");
        }

        if (bytes.length > MAX_COVER_SIZE) {
            throw new BusinessException(400, "路线封面大小不能超过5MB");
        }

        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new BusinessException(400, "路线封面仅支持 JPG、PNG、WEBP 格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return storageService.store(StorageCategory.ROUTE, filename, bytes);
    }

    private void deleteImageQuietly(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith(IMAGE_URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(IMAGE_URL_PREFIX.length());
        if (!StringUtils.hasText(filename)) {
            return;
        }
        storageService.delete(StorageCategory.ROUTE, filename);
    }

    private boolean savedImageUrlEqualsOriginal(String savedImageUrl, String originalImageUrl) {
        if (savedImageUrl == null) {
            return originalImageUrl == null;
        }
        return savedImageUrl.equals(originalImageUrl);
    }

    private String detectImageExtension(byte[] bytes) {
        if (isPng(bytes)) {
            return "png";
        }
        if (isJpeg(bytes)) {
            return "jpg";
        }
        if (isWebp(bytes)) {
            return "webp";
        }
        return null;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && readAscii(bytes, 0, 4).equals("RIFF")
                && readAscii(bytes, 8, 4).equals("WEBP");
    }

    private String readAscii(byte[] bytes, int start, int length) {
        return new String(bytes, start, length, StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
    }
}
