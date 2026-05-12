package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.dto.response.POIShareResponse;
import com.smartcampus.dto.response.UserCheckInItemResponse;
import com.smartcampus.dto.response.UserPublicProfileResponse;
import com.smartcampus.entity.POICheckIn;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareLike;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POICheckInRepository;
import com.smartcampus.repository.POIReviewRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.UserPublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPublicProfileServiceImpl implements UserPublicProfileService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_REPLY_SIZE = 3;

    private final UserRepository userRepository;
    private final POICheckInRepository poiCheckInRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIReviewRepository poiReviewRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(Long userId) {
        User user = getRequiredUser(userId);

        long checkInCount = poiCheckInRepository.countByUserId(userId);
        long shareCount = poiShareRepository.countByUserId(userId);
        long receivedLikeCount = poiShareLikeRepository.countByUserId(userId);
        long reviewCount = poiReviewRepository.countByUserId(userId);

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = "/" + avatarUrl;
        }

        return new UserPublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                avatarUrl,
                user.getBio(),
                user.getCreatedAt(),
                checkInCount,
                shareCount,
                receivedLikeCount,
                reviewCount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIShareResponse> getUserPublicShares(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIShare> sharePage = poiShareRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<POIShare> shares = sharePage.getContent();

        List<Long> shareIds = shares.stream().map(POIShare::getId).toList();

        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        List<POIShareResponse> records = shares.stream()
                .map(share -> POIShareResponse.fromEntity(
                        share,
                        null,
                        null,
                        likeCountMap.getOrDefault(share.getId(), 0L),
                        false,
                        replyCountMap.getOrDefault(share.getId(), 0L),
                        List.of()
                ))
                .toList();

        return new PageResponse<>(records, sharePage.getNumber(), sharePage.getSize(), sharePage.getTotalElements(), sharePage.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserCheckInItemResponse> getUserCheckIns(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POICheckIn> result = poiCheckInRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UserCheckInItemResponse> records = result.getContent().stream()
                .map(this::toCheckInItemResponse)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private UserCheckInItemResponse toCheckInItemResponse(POICheckIn checkIn) {
        return new UserCheckInItemResponse(
                checkIn.getId(),
                checkIn.getPoi().getId(),
                checkIn.getPoi().getName(),
                checkIn.getPoi().getCategory(),
                checkIn.getPoi().getLatitude(),
                checkIn.getPoi().getLongitude(),
                checkIn.getCreatedAt()
        );
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }
}
