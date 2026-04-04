package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminActivityDetailResponse;
import com.smartcampus.dto.response.AdminActivityListItemResponse;
import com.smartcampus.entity.Activity;
import com.smartcampus.entity.POI;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.ActivityRepository;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.service.AdminActivityService;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.util.ImageThumbnailUtils;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 220;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final long MAX_COVER_SIZE = 5L * 1024 * 1024;
    private static final String IMAGE_URL_PREFIX = "/uploads/activities/";
    private static final int COVER_THUMB_MAX_WIDTH = 480;
    private static final int COVER_THUMB_MAX_HEIGHT = 320;

    private final ActivityRepository activityRepository;
    private final POIRepository poiRepository;
    private final AdminOperationLogService adminOperationLogService;

    @Value("${app.upload.activity-dir:uploads/activities}")
    private String activityUploadDir;

    private Path activityStoragePath;

    @PostConstruct
    public void init() throws IOException {
        activityStoragePath = Paths.get(activityUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(activityStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminActivityListItemResponse> getActivities(
            String keyword,
            Short status,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    ) {
        validateStatusIfPresent(status);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Order.asc("startTime"), Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );

        Page<Activity> result = activityRepository.findAll(buildSpecification(keyword, status, poiId, startTime, endTime), pageable);
        List<AdminActivityListItemResponse> records = result.getContent().stream()
                .map(AdminActivityListItemResponse::fromEntity)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminActivityDetailResponse getActivityDetail(Long activityId) {
        return AdminActivityDetailResponse.fromEntity(getRequiredActivity(activityId));
    }

    @Override
    @Transactional
    public AdminActivityDetailResponse createActivity(
            String title,
            String summary,
            String content,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Short status,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        Activity activity = new Activity();
        applyActivityPayload(activity, title, summary, content, poiId, startTime, endTime, status);

        String savedImageUrl = null;
        try {
            savedImageUrl = saveImageIfPresent(coverImage);
            activity.setCoverImageUrl(savedImageUrl);
            activity = activityRepository.save(activity);
            adminOperationLogService.record(
                    operatorUserId,
                    "活动管理",
                    "创建活动",
                    "活动",
                    activity.getId(),
                    "创建活动《" + activity.getTitle() + "》"
            );
            return AdminActivityDetailResponse.fromEntity(activity);
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AdminActivityDetailResponse updateActivity(
            Long activityId,
            String title,
            String summary,
            String content,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Short status,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        Activity activity = getRequiredActivity(activityId);
        String originalCoverImageUrl = activity.getCoverImageUrl();
        String savedImageUrl = null;

        applyActivityPayload(activity, title, summary, content, poiId, startTime, endTime, status);

        try {
            if (Boolean.TRUE.equals(removeCoverImage)) {
                activity.setCoverImageUrl(null);
            }
            if (coverImage != null && !coverImage.isEmpty()) {
                savedImageUrl = saveImageIfPresent(coverImage);
                activity.setCoverImageUrl(savedImageUrl);
            }
            activity = activityRepository.save(activity);

            if ((Boolean.TRUE.equals(removeCoverImage) || savedImageUrl != null) && !savedImageUrlEqualsOriginal(savedImageUrl, originalCoverImageUrl)) {
                deleteImageQuietly(originalCoverImageUrl);
            }

            adminOperationLogService.record(
                    operatorUserId,
                    "活动管理",
                    "更新活动",
                    "活动",
                    activity.getId(),
                    "更新活动《" + activity.getTitle() + "》"
            );
            return AdminActivityDetailResponse.fromEntity(activity);
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            activity.setCoverImageUrl(originalCoverImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AdminActivityDetailResponse updatePublishStatus(Long activityId, Boolean published, Long operatorUserId) {
        if (published == null) {
            throw new BusinessException(400, "发布状态不能为空");
        }

        Activity activity = getRequiredActivity(activityId);
        if (published) {
            activity.setStatus(Activity.STATUS_PUBLISHED);
            activity.setPublishedAt(LocalDateTime.now());
        } else {
            activity.setStatus(Activity.STATUS_DRAFT);
        }
        Activity savedActivity = activityRepository.save(activity);
        adminOperationLogService.record(
                operatorUserId,
                "活动管理",
                published ? "发布活动" : "取消发布活动",
                "活动",
                savedActivity.getId(),
                (published ? "发布活动《" : "取消发布活动《") + savedActivity.getTitle() + "》"
        );
        return AdminActivityDetailResponse.fromEntity(savedActivity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId, Long operatorUserId) {
        Activity activity = getRequiredActivity(activityId);
        Long targetId = activity.getId();
        String title = activity.getTitle();
        String coverImageUrl = activity.getCoverImageUrl();
        activityRepository.delete(activity);
        deleteImageQuietly(coverImageUrl);
        adminOperationLogService.record(
                operatorUserId,
                "活动管理",
                "删除活动",
                "活动",
                targetId,
                "删除活动《" + title + "》"
        );
    }

    private Specification<Activity> buildSpecification(
            String keyword,
            Short status,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                var poiJoin = root.join("poi", JoinType.LEFT);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(poiJoin.get("name")), normalizedKeyword)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (poiId != null) {
                predicates.add(criteriaBuilder.equal(root.join("poi", JoinType.LEFT).get("id"), poiId));
            }

            if (startTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startTime));
            }

            if (endTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Activity getRequiredActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(404, "活动不存在"));
    }

    private void applyActivityPayload(
            Activity activity,
            String title,
            String summary,
            String content,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Short status
    ) {
        String normalizedTitle = normalizeText(title, "活动标题不能为空", MAX_TITLE_LENGTH, "活动标题不能超过120个字符");
        String normalizedSummary = normalizeText(summary, "活动摘要不能为空", MAX_SUMMARY_LENGTH, "活动摘要不能超过220个字符");
        String normalizedContent = normalizeText(content, "活动正文不能为空", MAX_CONTENT_LENGTH, "活动正文不能超过10000个字符");
        Short normalizedStatus = normalizeStatus(status);
        POI poi = resolvePoi(poiId);
        LocalDateTime normalizedStartTime = requireTime(startTime, "活动开始时间不能为空");
        LocalDateTime normalizedEndTime = requireTime(endTime, "活动结束时间不能为空");

        if (normalizedEndTime.isBefore(normalizedStartTime)) {
            throw new BusinessException(400, "活动结束时间不能早于开始时间");
        }

        activity.setTitle(normalizedTitle);
        activity.setSummary(normalizedSummary);
        activity.setContent(normalizedContent);
        activity.setPoi(poi);
        activity.setStartTime(normalizedStartTime);
        activity.setEndTime(normalizedEndTime);
        activity.setStatus(normalizedStatus);

        if (normalizedStatus == Activity.STATUS_PUBLISHED) {
            activity.setPublishedAt(LocalDateTime.now());
        }
    }

    private POI resolvePoi(Long poiId) {
        if (poiId == null) {
            return null;
        }

        return poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(400, "关联地点不存在"));
    }

    private LocalDateTime requireTime(LocalDateTime value, String message) {
        if (value == null) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    private String normalizeText(String value, String emptyMessage, int maxLength, String lengthMessage) {
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

    private Short normalizeStatus(Short status) {
        if (status == null) {
            return Activity.STATUS_DRAFT;
        }
        validateStatusIfPresent(status);
        return status;
    }

    private void validateStatusIfPresent(Short status) {
        if (status == null) {
            return;
        }
        if (status != Activity.STATUS_DRAFT && status != Activity.STATUS_PUBLISHED) {
            throw new BusinessException(400, "不支持的活动状态");
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
            throw new BusinessException(400, "活动封面读取失败");
        }

        if (bytes.length > MAX_COVER_SIZE) {
            throw new BusinessException(400, "活动封面大小不能超过5MB");
        }

        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new BusinessException(400, "活动封面仅支持 JPG、PNG、WEBP 格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = activityStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(activityStoragePath)) {
            throw new BusinessException(400, "非法的封面存储路径");
        }

        try {
            Files.write(targetPath, bytes);
            ImageThumbnailUtils.createThumbnailIfSupported(bytes, extension, targetPath, COVER_THUMB_MAX_WIDTH, COVER_THUMB_MAX_HEIGHT);
        } catch (IOException e) {
            throw new BusinessException(500, "活动封面保存失败");
        }

        return IMAGE_URL_PREFIX + filename;
    }

    private void deleteImageQuietly(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith(IMAGE_URL_PREFIX)) {
            return;
        }

        String filename = imageUrl.substring(IMAGE_URL_PREFIX.length());
        if (!StringUtils.hasText(filename)) {
            return;
        }

        Path targetPath = activityStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(activityStoragePath)) {
            return;
        }

        ImageThumbnailUtils.deleteImageAndThumbnailQuietly(targetPath);
    }

    private boolean savedImageUrlEqualsOriginal(String savedImageUrl, String originalCoverImageUrl) {
        if (savedImageUrl == null) {
            return originalCoverImageUrl == null;
        }
        return savedImageUrl.equals(originalCoverImageUrl);
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
