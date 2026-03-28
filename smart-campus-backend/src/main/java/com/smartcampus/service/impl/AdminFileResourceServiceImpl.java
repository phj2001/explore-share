package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminFileResourceListItemResponse;
import com.smartcampus.entity.Announcement;
import com.smartcampus.entity.POIShareImage;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AnnouncementRepository;
import com.smartcampus.repository.POIShareImageRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.AdminFileResourceService;
import com.smartcampus.service.AdminOperationLogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminFileResourceServiceImpl implements AdminFileResourceService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final String TYPE_AVATAR = "AVATAR";
    private static final String TYPE_SHARE_IMAGE = "SHARE_IMAGE";
    private static final String TYPE_ANNOUNCEMENT_COVER = "ANNOUNCEMENT_COVER";

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_ORPHAN_FILE = "ORPHAN_FILE";
    private static final String STATUS_MISSING_FILE = "MISSING_FILE";

    private static final String AVATAR_PREFIX = "/uploads/avatars/";
    private static final String SHARE_IMAGE_PREFIX = "/uploads/poi-shares/";
    private static final String ANNOUNCEMENT_PREFIX = "/uploads/announcements/";

    private final UserRepository userRepository;
    private final POIShareImageRepository poiShareImageRepository;
    private final AnnouncementRepository announcementRepository;
    private final AdminOperationLogService adminOperationLogService;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarUploadDir;

    @Value("${app.upload.poi-share-dir:uploads/poi-shares}")
    private String poiShareUploadDir;

    @Value("${app.upload.announcement-dir:uploads/announcements}")
    private String announcementUploadDir;

    private Path avatarStoragePath;
    private Path poiShareStoragePath;
    private Path announcementStoragePath;

    @PostConstruct
    public void init() throws IOException {
        avatarStoragePath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        poiShareStoragePath = Paths.get(poiShareUploadDir).toAbsolutePath().normalize();
        announcementStoragePath = Paths.get(announcementUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(avatarStoragePath);
        Files.createDirectories(poiShareStoragePath);
        Files.createDirectories(announcementStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminFileResourceListItemResponse> getResources(String keyword, String resourceType, String status, Integer page, Integer size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedType = normalizeOptional(resourceType);
        String normalizedStatus = normalizeOptional(status);

        List<AdminFileResourceListItemResponse> allResources = collectResources();
        List<AdminFileResourceListItemResponse> filtered = allResources.stream()
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .filter(item -> matchesType(item, normalizedType))
                .filter(item -> matchesStatus(item, normalizedStatus))
                .sorted(buildComparator())
                .toList();

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        int fromIndex = Math.min(pageNo * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<AdminFileResourceListItemResponse> records = filtered.subList(fromIndex, toIndex);

        return new PageResponse<>(records, pageNo, pageSize, (long) filtered.size(), toIndex < filtered.size());
    }

    @Override
    @Transactional
    public void deleteResource(String resourceType, String resourceUrl, Long ownerId, Long operatorUserId) {
        String normalizedType = requireText(resourceType, "资源类型不能为空").toUpperCase(Locale.ROOT);
        String normalizedUrl = requireText(resourceUrl, "资源地址不能为空");

        switch (normalizedType) {
            case TYPE_AVATAR -> deleteAvatarResource(normalizedUrl, ownerId, operatorUserId);
            case TYPE_SHARE_IMAGE -> deleteShareImageResource(normalizedUrl, ownerId, operatorUserId);
            case TYPE_ANNOUNCEMENT_COVER -> deleteAnnouncementCoverResource(normalizedUrl, ownerId, operatorUserId);
            default -> throw new BusinessException(400, "不支持的资源类型");
        }
    }

    private List<AdminFileResourceListItemResponse> collectResources() {
        Map<String, AdminFileResourceListItemResponse> resourceMap = new LinkedHashMap<>();

        collectAvatarResources(resourceMap);
        collectShareImageResources(resourceMap);
        collectAnnouncementCoverResources(resourceMap);
        collectOrphanFiles(resourceMap, avatarStoragePath, TYPE_AVATAR, AVATAR_PREFIX);
        collectOrphanFiles(resourceMap, poiShareStoragePath, TYPE_SHARE_IMAGE, SHARE_IMAGE_PREFIX);
        collectOrphanFiles(resourceMap, announcementStoragePath, TYPE_ANNOUNCEMENT_COVER, ANNOUNCEMENT_PREFIX);

        return new ArrayList<>(resourceMap.values());
    }

    private void collectAvatarResources(Map<String, AdminFileResourceListItemResponse> resourceMap) {
        for (User user : userRepository.findAll()) {
            if (!StringUtils.hasText(user.getAvatarUrl())) {
                continue;
            }
            addReferencedResource(
                    resourceMap,
                    TYPE_AVATAR,
                    user.getAvatarUrl(),
                    "USER",
                    user.getId(),
                    buildUserOwnerName(user)
            );
        }
    }

    private void collectShareImageResources(Map<String, AdminFileResourceListItemResponse> resourceMap) {
        for (POIShareImage image : poiShareImageRepository.findAll()) {
            if (!StringUtils.hasText(image.getImageUrl())) {
                continue;
            }
            String ownerName = "分享 #" + image.getShare().getId() + " · " + buildPreview(image.getShare().getContent());
            addReferencedResource(resourceMap, TYPE_SHARE_IMAGE, image.getImageUrl(), "SHARE", image.getShare().getId(), ownerName);
        }
    }

    private void collectAnnouncementCoverResources(Map<String, AdminFileResourceListItemResponse> resourceMap) {
        for (Announcement announcement : announcementRepository.findAll()) {
            if (!StringUtils.hasText(announcement.getCoverImageUrl())) {
                continue;
            }
            addReferencedResource(
                    resourceMap,
                    TYPE_ANNOUNCEMENT_COVER,
                    announcement.getCoverImageUrl(),
                    "ANNOUNCEMENT",
                    announcement.getId(),
                    announcement.getTitle()
            );
        }
    }

    private void collectOrphanFiles(Map<String, AdminFileResourceListItemResponse> resourceMap, Path directory, String resourceType, String urlPrefix) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                String filename = file.getFileName().toString();
                String url = urlPrefix + filename;
                String key = buildKey(resourceType, url, null);
                if (resourceMap.containsKey(key)) {
                    continue;
                }
                resourceMap.put(key, buildResourceItem(resourceType, url, null, null, null, false, file));
            }
        } catch (IOException ignored) {
        }
    }

    private void addReferencedResource(
            Map<String, AdminFileResourceListItemResponse> resourceMap,
            String resourceType,
            String resourceUrl,
            String ownerType,
            Long ownerId,
            String ownerName
    ) {
        Path filePath = resolveFilePath(resourceType, resourceUrl);
        resourceMap.put(
                buildKey(resourceType, resourceUrl, ownerId),
                buildResourceItem(resourceType, resourceUrl, ownerType, ownerId, ownerName, true, filePath)
        );
    }

    private AdminFileResourceListItemResponse buildResourceItem(
            String resourceType,
            String resourceUrl,
            String ownerType,
            Long ownerId,
            String ownerName,
            boolean referenced,
            Path filePath
    ) {
        boolean fileExists = filePath != null && Files.exists(filePath);
        Long fileSize = null;
        LocalDateTime lastModifiedAt = null;

        if (fileExists) {
            try {
                fileSize = Files.size(filePath);
                lastModifiedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(Files.getLastModifiedTime(filePath).toMillis()),
                        ZoneId.systemDefault()
                );
            } catch (IOException ignored) {
            }
        }

        return new AdminFileResourceListItemResponse(
                resourceType,
                resourceUrl,
                extractFilename(resourceUrl),
                fileSize,
                lastModifiedAt,
                ownerType,
                ownerId,
                ownerName,
                referenced,
                fileExists,
                determineStatus(referenced, fileExists)
        );
    }

    private void deleteAvatarResource(String resourceUrl, Long ownerId, Long operatorUserId) {
        List<User> matchedUsers = userRepository.findByAvatarUrl(resourceUrl);
        if (ownerId != null) {
            matchedUsers = matchedUsers.stream().filter(user -> ownerId.equals(user.getId())).toList();
        }

        if (!matchedUsers.isEmpty()) {
            for (User user : matchedUsers) {
                user.setAvatarUrl(null);
            }
            userRepository.saveAll(matchedUsers);
        }

        deleteFileByType(TYPE_AVATAR, resourceUrl);
        recordDeleteLog(operatorUserId, TYPE_AVATAR, resourceUrl, ownerId);
    }

    private void deleteShareImageResource(String resourceUrl, Long ownerId, Long operatorUserId) {
        List<POIShareImage> matchedImages = poiShareImageRepository.findByImageUrl(resourceUrl);
        if (ownerId != null) {
            matchedImages = matchedImages.stream().filter(image -> ownerId.equals(image.getShare().getId())).toList();
        }

        if (!matchedImages.isEmpty()) {
            poiShareImageRepository.deleteAll(matchedImages);
        }

        deleteFileByType(TYPE_SHARE_IMAGE, resourceUrl);
        recordDeleteLog(operatorUserId, TYPE_SHARE_IMAGE, resourceUrl, ownerId);
    }

    private void deleteAnnouncementCoverResource(String resourceUrl, Long ownerId, Long operatorUserId) {
        List<Announcement> matchedAnnouncements = announcementRepository.findByCoverImageUrl(resourceUrl);
        if (ownerId != null) {
            matchedAnnouncements = matchedAnnouncements.stream().filter(item -> ownerId.equals(item.getId())).toList();
        }

        if (!matchedAnnouncements.isEmpty()) {
            for (Announcement announcement : matchedAnnouncements) {
                announcement.setCoverImageUrl(null);
            }
            announcementRepository.saveAll(matchedAnnouncements);
        }

        deleteFileByType(TYPE_ANNOUNCEMENT_COVER, resourceUrl);
        recordDeleteLog(operatorUserId, TYPE_ANNOUNCEMENT_COVER, resourceUrl, ownerId);
    }

    private void recordDeleteLog(Long operatorUserId, String resourceType, String resourceUrl, Long ownerId) {
        adminOperationLogService.record(
                operatorUserId,
                "文件资源",
                "删除资源",
                mapResourceTypeLabel(resourceType),
                ownerId,
                "删除资源文件 " + extractFilename(resourceUrl)
        );
    }

    private void deleteFileByType(String resourceType, String resourceUrl) {
        Path path = resolveFilePath(resourceType, resourceUrl);
        if (path == null) {
            throw new BusinessException(400, "资源地址不合法");
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new BusinessException(500, "删除资源文件失败");
        }
    }

    private Comparator<AdminFileResourceListItemResponse> buildComparator() {
        Map<String, Integer> statusWeight = new HashMap<>();
        statusWeight.put(STATUS_MISSING_FILE, 0);
        statusWeight.put(STATUS_ORPHAN_FILE, 1);
        statusWeight.put(STATUS_NORMAL, 2);

        return Comparator
                .comparingInt((AdminFileResourceListItemResponse item) -> statusWeight.getOrDefault(item.getStatus(), 9))
                .thenComparing(AdminFileResourceListItemResponse::getLastModifiedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdminFileResourceListItemResponse::getFilename, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private boolean matchesKeyword(AdminFileResourceListItemResponse item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return Stream.of(
                        item.getFilename(),
                        item.getOwnerName(),
                        item.getOwnerType(),
                        item.getResourceType(),
                        item.getStatus()
                )
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(lowerKeyword));
    }

    private boolean matchesType(AdminFileResourceListItemResponse item, String resourceType) {
        return !StringUtils.hasText(resourceType) || resourceType.equalsIgnoreCase(item.getResourceType());
    }

    private boolean matchesStatus(AdminFileResourceListItemResponse item, String status) {
        return !StringUtils.hasText(status) || status.equalsIgnoreCase(item.getStatus());
    }

    private String determineStatus(boolean referenced, boolean fileExists) {
        if (referenced && fileExists) {
            return STATUS_NORMAL;
        }
        if (referenced) {
            return STATUS_MISSING_FILE;
        }
        return STATUS_ORPHAN_FILE;
    }

    private Path resolveFilePath(String resourceType, String resourceUrl) {
        if (!StringUtils.hasText(resourceUrl)) {
            return null;
        }

        String prefix = switch (resourceType) {
            case TYPE_AVATAR -> AVATAR_PREFIX;
            case TYPE_SHARE_IMAGE -> SHARE_IMAGE_PREFIX;
            case TYPE_ANNOUNCEMENT_COVER -> ANNOUNCEMENT_PREFIX;
            default -> null;
        };

        Path storagePath = switch (resourceType) {
            case TYPE_AVATAR -> avatarStoragePath;
            case TYPE_SHARE_IMAGE -> poiShareStoragePath;
            case TYPE_ANNOUNCEMENT_COVER -> announcementStoragePath;
            default -> null;
        };

        if (prefix == null || storagePath == null || !resourceUrl.startsWith(prefix)) {
            return null;
        }

        String filename = resourceUrl.substring(prefix.length());
        if (!StringUtils.hasText(filename)) {
            return null;
        }

        Path path = storagePath.resolve(filename).normalize();
        if (!path.startsWith(storagePath)) {
            return null;
        }
        return path;
    }

    private String buildKey(String resourceType, String resourceUrl, Long ownerId) {
        return resourceType + "|" + resourceUrl + "|" + (ownerId == null ? "-" : ownerId);
    }

    private String extractFilename(String resourceUrl) {
        if (!StringUtils.hasText(resourceUrl)) {
            return "-";
        }
        int index = resourceUrl.lastIndexOf('/');
        return index >= 0 ? resourceUrl.substring(index + 1) : resourceUrl;
    }

    private String buildUserOwnerName(User user) {
        String displayName = StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName().trim() : user.getUsername();
        return displayName + " @" + user.getUsername();
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "无文本内容";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 24 ? normalized : normalized.substring(0, 24) + "...";
    }

    private String mapResourceTypeLabel(String resourceType) {
        return switch (resourceType) {
            case TYPE_AVATAR -> "头像";
            case TYPE_SHARE_IMAGE -> "分享图片";
            case TYPE_ANNOUNCEMENT_COVER -> "公告封面";
            default -> "资源";
        };
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }
}
