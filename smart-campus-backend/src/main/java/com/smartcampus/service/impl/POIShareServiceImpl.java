package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIShareLikeResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.dto.response.POIShareResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareImage;
import com.smartcampus.entity.POIShareLike;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.RecommendedShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.UserRole;
import com.smartcampus.service.POIShareService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class POIShareServiceImpl implements POIShareService {

    private static final int MAX_CONTENT_LENGTH = 300;
    private static final int MAX_IMAGE_COUNT = 3;
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MAX_REPLY_LENGTH = 200;
    private static final int MAX_REPLY_PAGE_SIZE = 100;
    private static final String IMAGE_URL_PREFIX = "/uploads/poi-shares/";

    private final POIShareRepository poiShareRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIRepository poiRepository;
    private final UserRepository userRepository;
    private final RecommendedShareRepository recommendedShareRepository;

    @Value("${app.upload.poi-share-dir:uploads/poi-shares}")
    private String poiShareUploadDir;

    private Path poiShareStoragePath;

    @PostConstruct
    public void init() throws IOException {
        poiShareStoragePath = Paths.get(poiShareUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(poiShareStoragePath);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIShareResponse> getSharesByPoi(Long poiId, Integer page, Integer size, Long currentUserId) {
        ensurePoiExists(poiId);
        User currentUser = getOptionalUser(currentUserId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<POIShare> sharePage = poiShareRepository.findByPoiId(poiId, pageable);

        List<POIShare> shareRecords = sharePage.getContent();
        List<Long> shareIds = shareRecords.stream()
                .map(POIShare::getId)
                .toList();

        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : getCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : getCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));
        Set<Long> likedShareIds = getLikedShareIds(shareIds, currentUser != null ? currentUser.getId() : null);
        Map<Long, List<POIShareReplyResponse>> previewReplyMap = getPreviewReplyMap(
                shareRecords,
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getRole() : null
        );

        List<POIShareResponse> records = shareRecords.stream()
                .map(share -> POIShareResponse.fromEntity(
                        share,
                        currentUser != null ? currentUser.getId() : null,
                        currentUser != null ? currentUser.getRole() : null,
                        likeCountMap.getOrDefault(share.getId(), 0L),
                        likedShareIds.contains(share.getId()),
                        replyCountMap.getOrDefault(share.getId(), 0L),
                        previewReplyMap.getOrDefault(share.getId(), List.of())
                ))
                .toList();

        return new PageResponse<>(
                records,
                sharePage.getNumber(),
                sharePage.getSize(),
                sharePage.getTotalElements(),
                sharePage.hasNext()
        );
    }

    @Override
    @Transactional
    public POIShareResponse createShare(Long poiId, Long userId, String content, List<MultipartFile> images) {
        POI poi = poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(404, "POI不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        String normalizedContent = normalizeContent(content);
        List<MultipartFile> validImages = normalizeImages(images);
        validateSharePayload(normalizedContent, validImages);

        List<String> savedImageUrls = new ArrayList<>();

        try {
            POIShare share = new POIShare();
            share.setPoi(poi);
            share.setUser(user);
            share.setContent(normalizedContent);

            for (int index = 0; index < validImages.size(); index++) {
                String imageUrl = saveImage(validImages.get(index));
                savedImageUrls.add(imageUrl);

                POIShareImage image = new POIShareImage();
                image.setImageUrl(imageUrl);
                image.setSortOrder(index);
                share.addImage(image);
            }

            POIShare savedShare = poiShareRepository.save(share);
            return POIShareResponse.fromEntity(savedShare, user.getId(), user.getRole(), 0L, false, 0L, List.of());
        } catch (RuntimeException ex) {
            savedImageUrls.forEach(this::deleteImageQuietly);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteShare(Long shareId, Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        POIShare share = poiShareRepository.findWithUserAndImagesById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));

        validateShareDeletePermission(share, currentUser);

        List<String> imageUrls = share.getImages().stream()
                .map(POIShareImage::getImageUrl)
                .toList();

        poiShareLikeRepository.deleteByShareId(shareId);
        poiShareReplyRepository.deleteByShareId(shareId);
        recommendedShareRepository.deleteByShareId(shareId);
        poiShareRepository.delete(share);
        imageUrls.forEach(this::deleteImageQuietly);
    }

    @Override
    @Transactional
    public POIShareLikeResponse likeShare(Long shareId, Long userId) {
        POIShare share = poiShareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (!poiShareLikeRepository.existsByShareIdAndUserId(shareId, userId)) {
            POIShareLike like = new POIShareLike();
            like.setShare(share);
            like.setUser(user);
            poiShareLikeRepository.save(like);
        }

        return new POIShareLikeResponse(shareId, poiShareLikeRepository.countByShareId(shareId), true);
    }

    @Override
    @Transactional
    public POIShareLikeResponse unlikeShare(Long shareId, Long userId) {
        ensureShareExists(shareId);
        poiShareLikeRepository.findByShareIdAndUserId(shareId, userId)
                .ifPresent(poiShareLikeRepository::delete);
        return new POIShareLikeResponse(shareId, poiShareLikeRepository.countByShareId(shareId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIShareReplyResponse> getRepliesByShare(Long shareId, Integer page, Integer size, Long currentUserId) {
        ensureShareExists(shareId);
        User currentUser = getOptionalUser(currentUserId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 3 : size, 1), MAX_REPLY_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<POIShareReply> replyPage = poiShareReplyRepository.findByShareIdOrderByCreatedAtAscIdAsc(shareId, pageable);

        List<POIShareReplyResponse> records = replyPage.getContent().stream()
                .map(reply -> POIShareReplyResponse.fromEntity(
                        reply,
                        currentUser != null ? currentUser.getId() : null,
                        currentUser != null ? currentUser.getRole() : null
                ))
                .toList();

        return new PageResponse<>(
                records,
                replyPage.getNumber(),
                replyPage.getSize(),
                replyPage.getTotalElements(),
                replyPage.hasNext()
        );
    }

    @Override
    @Transactional
    public POIShareReplyResponse createReply(Long shareId, Long userId, String content) {
        POIShare share = poiShareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        String normalizedContent = normalizeReplyContent(content);

        POIShareReply reply = new POIShareReply();
        reply.setShare(share);
        reply.setUser(user);
        reply.setContent(normalizedContent);

        POIShareReply savedReply = poiShareReplyRepository.save(reply);
        return POIShareReplyResponse.fromEntity(savedReply, user.getId(), user.getRole());
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        POIShareReply reply = poiShareReplyRepository.findWithUserAndShareById(replyId)
                .orElseThrow(() -> new BusinessException(404, "回复不存在"));

        boolean isOwner = reply.getUser().getId().equals(userId);
        boolean isSuperAdmin = UserRole.fromCode(currentUser.getRole()) == UserRole.SUPER_ADMIN;
        if (!isOwner && !isSuperAdmin) {
            throw new BusinessException(403, "无权删除该回复");
        }

        poiShareReplyRepository.delete(reply);
    }

    private void validateShareDeletePermission(POIShare share, User currentUser) {
        boolean isOwner = share.getUser().getId().equals(currentUser.getId());
        boolean isSuperAdmin = UserRole.fromCode(currentUser.getRole()) == UserRole.SUPER_ADMIN;
        if (!isOwner && !isSuperAdmin) {
            throw new BusinessException(403, "无权删除该分享");
        }
    }

    private void ensurePoiExists(Long poiId) {
        if (!poiRepository.existsById(poiId)) {
            throw new BusinessException(404, "POI不存在");
        }
    }

    private void ensureShareExists(Long shareId) {
        if (!poiShareRepository.existsById(shareId)) {
            throw new BusinessException(404, "分享不存在");
        }
    }

    private User getOptionalUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "分享内容不能超过300个字符");
        }
        return normalized;
    }

    private String normalizeReplyContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        String normalized = content.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        if (normalized.length() > MAX_REPLY_LENGTH) {
            throw new BusinessException(400, "回复内容不能超过200个字符");
        }
        return normalized;
    }

    private List<MultipartFile> normalizeImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private void validateSharePayload(String content, List<MultipartFile> images) {
        if (!StringUtils.hasText(content) && images.isEmpty()) {
            throw new BusinessException(400, "分享内容和图片不能同时为空");
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(400, "每次最多上传3张图片");
        }
    }

    private Map<Long, Long> getCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private Set<Long> getLikedShareIds(Collection<Long> shareIds, Long userId) {
        if (userId == null || shareIds == null || shareIds.isEmpty()) {
            return Set.of();
        }
        return poiShareLikeRepository.findLikedShareIdsByUserId(shareIds, userId).stream()
                .collect(Collectors.toSet());
    }

    private Map<Long, List<POIShareReplyResponse>> getPreviewReplyMap(List<POIShare> shares, Long currentUserId, Short currentUserRole) {
        if (shares == null || shares.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<POIShareReplyResponse>> result = new HashMap<>();
        for (POIShare share : shares) {
            List<POIShareReplyResponse> previewReplies = poiShareReplyRepository.findTop3ByShareIdOrderByCreatedAtAscIdAsc(share.getId()).stream()
                    .map(reply -> POIShareReplyResponse.fromEntity(reply, currentUserId, currentUserRole))
                    .toList();
            result.put(share.getId(), previewReplies);
        }
        return result;
    }

    private String saveImage(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(400, "分享图片读取失败");
        }

        if (bytes.length > MAX_IMAGE_SIZE) {
            throw new BusinessException(400, "单张图片大小不能超过5MB");
        }

        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new BusinessException(400, "分享图片仅支持 JPG、PNG、WEBP 格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = poiShareStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(poiShareStoragePath)) {
            throw new BusinessException(400, "非法的图片存储路径");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "分享图片保存失败");
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

        Path targetPath = poiShareStoragePath.resolve(filename).normalize();
        if (!targetPath.startsWith(poiShareStoragePath)) {
            return;
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
        }
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
