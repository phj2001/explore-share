package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.CreatePOIReviewRequest;
import com.smartcampus.dto.response.POIRatingSummaryResponse;
import com.smartcampus.dto.response.POIReviewResponse;
import com.smartcampus.entity.POI;
import com.smartcampus.entity.POIReview;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIReviewRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.POIReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class POIReviewServiceImpl implements POIReviewService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 200;

    private final POIReviewRepository poiReviewRepository;
    private final POIRepository poiRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public POIRatingSummaryResponse getRatingSummary(Long poiId) {
        getRequiredPoi(poiId);
        double avgRating = poiReviewRepository.getAverageRatingByPoiId(poiId);
        long reviewCount = poiReviewRepository.countByPoiId(poiId);
        return new POIRatingSummaryResponse(Math.round(avgRating * 10.0) / 10.0, reviewCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIReviewResponse> getPoiReviews(Long poiId, Integer page, Integer size, Long currentUserId) {
        getRequiredPoi(poiId);
        Pageable pageable = buildPageable(page, size);
        Page<POIReview> result = poiReviewRepository.findByPoiIdOrderByCreatedAtDesc(poiId, pageable);

        List<POIReviewResponse> records = result.getContent().stream()
                .map(review -> toReviewResponse(review, currentUserId))
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional
    public POIReviewResponse createOrUpdateReview(Long poiId, Long userId, CreatePOIReviewRequest request) {
        POI poi = getRequiredPoi(poiId);
        User user = getRequiredUser(userId);

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException(400, "评分必须在 1-5 之间");
        }

        String content = request.getContent();
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "评价内容不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        POIReview review = poiReviewRepository.findByPoiIdAndUserId(poiId, userId)
                .orElseGet(() -> {
                    POIReview newReview = new POIReview();
                    newReview.setPoi(poi);
                    newReview.setUser(user);
                    return newReview;
                });

        review.setRating(request.getRating().shortValue());
        review.setContent(StringUtils.hasText(content) ? content.trim() : null);

        POIReview saved = poiReviewRepository.save(review);
        return toReviewResponse(saved, userId);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        POIReview review = poiReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(404, "评价不存在"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的评价");
        }

        poiReviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIReviewResponse> getUserReviews(Long userId, Integer page, Integer size) {
        getRequiredUser(userId);
        Pageable pageable = buildPageable(page, size);
        Page<POIReview> result = poiReviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<POIReviewResponse> records = result.getContent().stream()
                .map(review -> toReviewResponse(review, userId))
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<POIReviewResponse> getAdminReviews(String keyword, Long poiId, Integer minRating, Integer maxRating, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<POIReview> result = poiReviewRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (poiId != null) {
                predicates.add(cb.equal(root.get("poi").get("id"), poiId));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating.shortValue()));
            }

            if (maxRating != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), maxRating.shortValue()));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), pattern),
                        cb.like(cb.lower(root.get("poi").get("name")), pattern),
                        cb.like(cb.lower(root.get("user").get("username")), pattern),
                        cb.like(cb.lower(root.get("user").get("displayName")), pattern)
                ));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);

        List<POIReviewResponse> records = result.getContent().stream()
                .map(review -> toReviewResponse(review, null))
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        if (!poiReviewRepository.existsById(reviewId)) {
            throw new BusinessException(404, "评价不存在");
        }
        poiReviewRepository.deleteById(reviewId);
    }

    private POIReviewResponse toReviewResponse(POIReview review, Long currentUserId) {
        User author = review.getUser();
        POI poi = review.getPoi();
        String avatarUrl = author.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = "/" + avatarUrl;
        }

        return new POIReviewResponse(
                review.getId(),
                poi.getId(),
                poi.getName(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                avatarUrl,
                review.getRating(),
                review.getContent(),
                currentUserId != null && currentUserId.equals(author.getId()),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private POI getRequiredPoi(Long poiId) {
        return poiRepository.findById(poiId)
                .orElseThrow(() -> new BusinessException(404, "POI不存在"));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
