package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.FeedItemResponse;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareImage;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserFollowRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final POIShareRepository poiShareRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedItemResponse> getFeed(Long userId, Integer page, Integer size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        List<Long> followingIds = userFollowRepository.findFollowingIdByFollowerId(userId);
        if (followingIds.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), 0, DEFAULT_PAGE_SIZE, 0L, false);
        }

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIShare> result = poiShareRepository.findByUserIdInOrderByCreatedAtDesc(followingIds, pageable);

        List<FeedItemResponse> records = result.getContent().stream()
                .map(this::toFeedItem)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private FeedItemResponse toFeedItem(POIShare share) {
        User user = share.getUser();
        List<String> imageUrls = share.getImages() != null
                ? share.getImages().stream().map(POIShareImage::getImageUrl).toList()
                : Collections.emptyList();

        return new FeedItemResponse(
                "share",
                share.getId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                share.getPoi().getId(),
                share.getPoi().getName(),
                share.getPoi().getCategory(),
                share.getPoi().getLatitude(),
                share.getPoi().getLongitude(),
                share.getContent(),
                imageUrls,
                share.getLikes() != null ? share.getLikes().size() : 0,
                share.getReplies() != null ? share.getReplies().size() : 0,
                share.getCreatedAt()
        );
    }
}
