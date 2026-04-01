package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.request.AdminCreateRecommendedShareRequest;
import com.smartcampus.dto.request.AdminUpdateRecommendedShareRequest;
import com.smartcampus.dto.response.AdminRecommendedShareCandidateResponse;
import com.smartcampus.dto.response.AdminRecommendedShareListItemResponse;
import com.smartcampus.dto.response.RecommendedSharePublicResponse;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.RecommendedShare;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.RecommendedShareRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.RecommendedShareService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendedShareServiceImpl implements RecommendedShareService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PUBLIC_LIMIT = 4;
    private static final int MAX_PUBLIC_LIMIT = 8;

    private final RecommendedShareRepository recommendedShareRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIRepository poiRepository;
    private final AdminOperationLogService adminOperationLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRecommendedShareListItemResponse> getRecommendedShares(String keyword, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<RecommendedShare> recommendedPage = recommendedShareRepository.findAll(buildRecommendedSpecification(keyword), pageable);
        List<Long> shareIds = recommendedPage.getContent().stream().map(item -> item.getShare().getId()).toList();
        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        List<AdminRecommendedShareListItemResponse> records = recommendedPage.getContent().stream()
                .map(item -> AdminRecommendedShareListItemResponse.fromEntity(
                        item,
                        likeCountMap.getOrDefault(item.getShare().getId(), 0L),
                        replyCountMap.getOrDefault(item.getShare().getId(), 0L)
                ))
                .toList();

        return new PageResponse<>(
                records,
                recommendedPage.getNumber(),
                recommendedPage.getSize(),
                recommendedPage.getTotalElements(),
                recommendedPage.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRecommendedShareCandidateResponse> getCandidateShares(
            String keyword,
            Long poiId,
            Boolean recommended,
            Integer page,
            Integer size
    ) {
        validatePoiIfPresent(poiId);
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Set<Long> recommendedShareIds = recommendedShareRepository.findAll().stream()
                .map(item -> item.getShare().getId())
                .collect(Collectors.toSet());

        if (Boolean.TRUE.equals(recommended) && recommendedShareIds.isEmpty()) {
            return new PageResponse<AdminRecommendedShareCandidateResponse>(
                    List.of(),
                    pageNo,
                    pageSize,
                    0L,
                    false
            );
        }

        Page<POIShare> sharePage = poiShareRepository.findAll(buildCandidateSpecification(keyword, poiId, recommended, recommendedShareIds), pageable);
        List<Long> shareIds = sharePage.getContent().stream().map(POIShare::getId).toList();
        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        List<AdminRecommendedShareCandidateResponse> records = sharePage.getContent().stream()
                .map(share -> AdminRecommendedShareCandidateResponse.fromEntity(
                        share,
                        likeCountMap.getOrDefault(share.getId(), 0L),
                        replyCountMap.getOrDefault(share.getId(), 0L),
                        recommendedShareIds.contains(share.getId())
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
    public AdminRecommendedShareListItemResponse createRecommendedShare(AdminCreateRecommendedShareRequest request, Long operatorUserId) {
        if (recommendedShareRepository.existsByShareId(request.getShareId())) {
            throw new BusinessException(400, "该分享已加入推荐内容");
        }

        POIShare share = poiShareRepository.findWithPoiUserAndImagesById(request.getShareId())
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));

        RecommendedShare recommendedShare = new RecommendedShare();
        recommendedShare.setShare(share);
        recommendedShare.setSortOrder(request.getSortOrder());
        recommendedShare.setRecommendationText(normalizeRecommendationText(request.getRecommendationText()));
        RecommendedShare saved = recommendedShareRepository.save(recommendedShare);

        adminOperationLogService.record(
                operatorUserId,
                "推荐内容",
                "新增推荐分享",
                "推荐分享",
                saved.getId(),
                "将分享 #" + share.getId() + " 加入推荐内容"
        );

        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminRecommendedShareListItemResponse updateRecommendedShare(Long id, AdminUpdateRecommendedShareRequest request, Long operatorUserId) {
        RecommendedShare recommendedShare = recommendedShareRepository.findWithShareById(id)
                .orElseThrow(() -> new BusinessException(404, "推荐内容不存在"));

        recommendedShare.setSortOrder(request.getSortOrder());
        recommendedShare.setRecommendationText(normalizeRecommendationText(request.getRecommendationText()));
        RecommendedShare saved = recommendedShareRepository.save(recommendedShare);

        adminOperationLogService.record(
                operatorUserId,
                "推荐内容",
                "更新推荐分享",
                "推荐分享",
                saved.getId(),
                "更新推荐分享 #" + saved.getId() + " 的排序和推荐语"
        );

        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRecommendedShare(Long id, Long operatorUserId) {
        RecommendedShare recommendedShare = recommendedShareRepository.findWithShareById(id)
                .orElseThrow(() -> new BusinessException(404, "推荐内容不存在"));

        String summary = "取消推荐分享 #" + recommendedShare.getShare().getId();
        recommendedShareRepository.delete(recommendedShare);

        adminOperationLogService.record(
                operatorUserId,
                "推荐内容",
                "取消推荐分享",
                "推荐分享",
                id,
                summary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedSharePublicResponse> getPublicRecommendedShares(Integer limit) {
        int size = Math.min(Math.max(limit == null ? DEFAULT_PUBLIC_LIMIT : limit, 1), MAX_PUBLIC_LIMIT);
        List<RecommendedShare> recommendedShares = recommendedShareRepository.findAllByOrderBySortOrderAscIdDesc(PageRequest.of(0, size));
        List<Long> shareIds = recommendedShares.stream().map(item -> item.getShare().getId()).toList();
        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        return recommendedShares.stream()
                .map(item -> RecommendedSharePublicResponse.fromEntity(
                        item,
                        likeCountMap.getOrDefault(item.getShare().getId(), 0L),
                        replyCountMap.getOrDefault(item.getShare().getId(), 0L)
                ))
                .toList();
    }

    private AdminRecommendedShareListItemResponse toAdminResponse(RecommendedShare recommendedShare) {
        Long shareId = recommendedShare.getShare().getId();
        return AdminRecommendedShareListItemResponse.fromEntity(
                recommendedShare,
                poiShareLikeRepository.countByShareId(shareId),
                poiShareReplyRepository.countByShareId(shareId)
        );
    }

    private Specification<RecommendedShare> buildRecommendedSpecification(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            Join<Object, Object> shareJoin = root.join("share");
            Join<Object, Object> userJoin = shareJoin.join("user");
            Join<Object, Object> poiJoin = shareJoin.join("poi");

            String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(shareJoin.get("content"), "")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("recommendationText"), "")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(userJoin.get("displayName"), "")), normalizedKeyword),
                    criteriaBuilder.like(criteriaBuilder.lower(poiJoin.get("name")), normalizedKeyword)
            );
        };
    }

    private Specification<POIShare> buildCandidateSpecification(
            String keyword,
            Long poiId,
            Boolean recommended,
            Set<Long> recommendedShareIds
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> userJoin = root.join("user");
            Join<Object, Object> poiJoin = root.join("poi");

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("content"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(userJoin.get("displayName"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(poiJoin.get("name")), normalizedKeyword)
                ));
            }

            if (poiId != null) {
                predicates.add(criteriaBuilder.equal(poiJoin.get("id"), poiId));
            }

            if (Boolean.TRUE.equals(recommended)) {
                predicates.add(root.get("id").in(recommendedShareIds));
            } else if (Boolean.FALSE.equals(recommended) && !recommendedShareIds.isEmpty()) {
                predicates.add(criteriaBuilder.not(root.get("id").in(recommendedShareIds)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validatePoiIfPresent(Long poiId) {
        if (poiId != null && !poiRepository.existsById(poiId)) {
            throw new BusinessException(404, "POI 不存在");
        }
    }

    private String normalizeRecommendationText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            Number key = (Number) row[0];
            Number value = (Number) row[1];
            result.put(key.longValue(), value.longValue());
        }
        return result;
    }
}
