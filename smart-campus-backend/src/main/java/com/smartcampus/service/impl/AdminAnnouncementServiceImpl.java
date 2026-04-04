package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminAnnouncementDetailResponse;
import com.smartcampus.dto.response.AdminAnnouncementListItemResponse;
import com.smartcampus.entity.Announcement;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AnnouncementRepository;
import com.smartcampus.service.AdminAnnouncementService;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.util.ImageThumbnailUtils;
import jakarta.annotation.PostConstruct;
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
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 220;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final long MAX_COVER_SIZE = 5L * 1024 * 1024;
    private static final String IMAGE_URL_PREFIX = "/uploads/announcements/";
    private static final int COVER_THUMB_MAX_WIDTH = 480;
    private static final int COVER_THUMB_MAX_HEIGHT = 320;

    private final AnnouncementRepository announcementRepository;
    private final AdminOperationLogService adminOperationLogService;

    @Value("${app.upload.announcement-dir:uploads/announcements}")
    private String announcementUploadDir;

    private Path announcementStoragePath;

    @PostConstruct
    public void init() throws IOException {
        announcementStoragePath = Paths.get(announcementUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(announcementStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAnnouncementListItemResponse> getAnnouncements(
            String keyword,
            Short status,
            Boolean pinned,
            Integer page,
            Integer size
    ) {
        validateStatusIfPresent(status);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("publishedAt"), Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );

        Page<Announcement> result = announcementRepository.findAll(buildSpecification(keyword, status, pinned), pageable);
        List<AdminAnnouncementListItemResponse> records = result.getContent().stream()
                .map(AdminAnnouncementListItemResponse::fromEntity)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnnouncementDetailResponse getAnnouncementDetail(Long announcementId) {
        return AdminAnnouncementDetailResponse.fromEntity(getRequiredAnnouncement(announcementId));
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse createAnnouncement(
            String title,
            String summary,
            String content,
            Boolean pinned,
            Short status,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        Announcement announcement = new Announcement();
        applyAnnouncementPayload(announcement, title, summary, content, pinned, status);

        String savedImageUrl = null;
        try {
            savedImageUrl = saveImageIfPresent(coverImage);
            announcement.setCoverImageUrl(savedImageUrl);
            announcement = announcementRepository.save(announcement);
            adminOperationLogService.record(
                    operatorUserId,
                    "公告管理",
                    "创建公告",
                    "公告",
                    announcement.getId(),
                    "创建公告《" + announcement.getTitle() + "》"
            );
            return AdminAnnouncementDetailResponse.fromEntity(announcement);
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse updateAnnouncement(
            Long announcementId,
            String title,
            String summary,
            String content,
            Boolean pinned,
            Short status,
            Boolean removeCoverImage,
            Long operatorUserId,
            MultipartFile coverImage
    ) {
        Announcement announcement = getRequiredAnnouncement(announcementId);
        String originalCoverImageUrl = announcement.getCoverImageUrl();
        String savedImageUrl = null;

        applyAnnouncementPayload(announcement, title, summary, content, pinned, status);

        try {
            if (Boolean.TRUE.equals(removeCoverImage)) {
                announcement.setCoverImageUrl(null);
            }
            if (coverImage != null && !coverImage.isEmpty()) {
                savedImageUrl = saveImageIfPresent(coverImage);
                announcement.setCoverImageUrl(savedImageUrl);
            }
            announcement = announcementRepository.save(announcement);

            if ((Boolean.TRUE.equals(removeCoverImage) || savedImageUrl != null) && !savedImageUrlEqualsOriginal(savedImageUrl, originalCoverImageUrl)) {
                deleteImageQuietly(originalCoverImageUrl);
            }

            adminOperationLogService.record(
                    operatorUserId,
                    "公告管理",
                    "更新公告",
                    "公告",
                    announcement.getId(),
                    "更新公告《" + announcement.getTitle() + "》"
            );
            return AdminAnnouncementDetailResponse.fromEntity(announcement);
        } catch (RuntimeException ex) {
            deleteImageQuietly(savedImageUrl);
            announcement.setCoverImageUrl(originalCoverImageUrl);
            throw ex;
        }
    }

    private boolean savedImageUrlEqualsOriginal(String savedImageUrl, String originalCoverImageUrl) {
        if (savedImageUrl == null) {
            return originalCoverImageUrl == null;
        }
        return savedImageUrl.equals(originalCoverImageUrl);
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse updatePublishStatus(Long announcementId, Boolean published, Long operatorUserId) {
        if (published == null) {
            throw new BusinessException(400, "发布状态不能为空");
        }

        Announcement announcement = getRequiredAnnouncement(announcementId);
        if (published) {
            announcement.setStatus(Announcement.STATUS_PUBLISHED);
            announcement.setPublishedAt(LocalDateTime.now());
        } else {
            announcement.setStatus(Announcement.STATUS_DRAFT);
        }
        Announcement savedAnnouncement = announcementRepository.save(announcement);
        adminOperationLogService.record(
                operatorUserId,
                "公告管理",
                published ? "发布公告" : "取消发布公告",
                "公告",
                savedAnnouncement.getId(),
                (published ? "发布公告《" : "取消发布公告《") + savedAnnouncement.getTitle() + "》"
        );
        return AdminAnnouncementDetailResponse.fromEntity(savedAnnouncement);
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse updatePinnedStatus(Long announcementId, Boolean pinned, Long operatorUserId) {
        if (pinned == null) {
            throw new BusinessException(400, "置顶状态不能为空");
        }

        Announcement announcement = getRequiredAnnouncement(announcementId);
        announcement.setPinned(pinned);
        Announcement savedAnnouncement = announcementRepository.save(announcement);
        adminOperationLogService.record(
                operatorUserId,
                "公告管理",
                pinned ? "置顶公告" : "取消置顶公告",
                "公告",
                savedAnnouncement.getId(),
                (pinned ? "置顶公告《" : "取消置顶公告《") + savedAnnouncement.getTitle() + "》"
        );
        return AdminAnnouncementDetailResponse.fromEntity(savedAnnouncement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long announcementId, Long operatorUserId) {
        Announcement announcement = getRequiredAnnouncement(announcementId);
        Long targetId = announcement.getId();
        String title = announcement.getTitle();
        String coverImageUrl = announcement.getCoverImageUrl();
        announcementRepository.delete(announcement);
        deleteImageQuietly(coverImageUrl);
        adminOperationLogService.record(
                operatorUserId,
                "公告管理",
                "删除公告",
                "公告",
                targetId,
                "删除公告《" + title + "》"
        );
    }

    private Specification<Announcement> buildSpecification(String keyword, Short status, Boolean pinned) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), normalizedKeyword)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (pinned != null) {
                predicates.add(criteriaBuilder.equal(root.get("pinned"), pinned));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Announcement getRequiredAnnouncement(Long announcementId) {
        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(404, "公告不存在"));
    }

    private void applyAnnouncementPayload(
            Announcement announcement,
            String title,
            String summary,
            String content,
            Boolean pinned,
            Short status
    ) {
        String normalizedTitle = normalizeText(title, "公告标题不能为空", MAX_TITLE_LENGTH, "公告标题不能超过120个字符");
        String normalizedSummary = normalizeText(summary, "公告摘要不能为空", MAX_SUMMARY_LENGTH, "公告摘要不能超过220个字符");
        String normalizedContent = normalizeText(content, "公告正文不能为空", MAX_CONTENT_LENGTH, "公告正文不能超过10000个字符");
        Short normalizedStatus = normalizeStatus(status);

        announcement.setTitle(normalizedTitle);
        announcement.setSummary(normalizedSummary);
        announcement.setContent(normalizedContent);
        announcement.setPinned(Boolean.TRUE.equals(pinned));
        announcement.setStatus(normalizedStatus);

        if (normalizedStatus == Announcement.STATUS_PUBLISHED) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
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
            return Announcement.STATUS_DRAFT;
        }
        validateStatusIfPresent(status);
        return status;
    }

    private void validateStatusIfPresent(Short status) {
        if (status == null) {
            return;
        }
        if (status != Announcement.STATUS_DRAFT && status != Announcement.STATUS_PUBLISHED) {
            throw new BusinessException(400, "不支持的公告状态");
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
            throw new BusinessException(400, "公告封面读取失败");
        }

        if (bytes.length > MAX_COVER_SIZE) {
            throw new BusinessException(400, "公告封面大小不能超过5MB");
        }

        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new BusinessException(400, "公告封面仅支持 JPG、PNG、WEBP 格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = announcementStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(announcementStoragePath)) {
            throw new BusinessException(400, "非法的封面存储路径");
        }

        try {
            Files.write(targetPath, bytes);
            ImageThumbnailUtils.createThumbnailIfSupported(bytes, extension, targetPath, COVER_THUMB_MAX_WIDTH, COVER_THUMB_MAX_HEIGHT);
        } catch (IOException e) {
            throw new BusinessException(500, "公告封面保存失败");
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

        Path targetPath = announcementStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(announcementStoragePath)) {
            return;
        }

        ImageThumbnailUtils.deleteImageAndThumbnailQuietly(targetPath);
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
