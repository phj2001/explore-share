package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminReplyListItemResponse;
import com.smartcampus.dto.response.AdminShareDetailResponse;
import com.smartcampus.dto.response.AdminShareListItemResponse;
import com.smartcampus.dto.response.POIShareReplyResponse;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIRepository;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.security.UserRole;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.AdminContentService;
import com.smartcampus.service.POIShareService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl implements AdminContentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final POIRepository poiRepository;
    private final POIShareService poiShareService;
    private final AdminOperationLogService adminOperationLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminShareListItemResponse> getShares(
            String keyword,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    ) {
        validateTimeRange(startTime, endTime);
        validatePoiIfPresent(poiId);

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<POIShare> sharePage = poiShareRepository.findAll(buildShareSpecification(keyword, poiId, startTime, endTime), pageable);
        List<Long> shareIds = sharePage.getContent().stream().map(POIShare::getId).toList();
        Map<Long, Long> likeCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareLikeRepository.countGroupedByShareIds(shareIds));
        Map<Long, Long> replyCountMap = shareIds.isEmpty()
                ? Map.of()
                : toCountMap(poiShareReplyRepository.countGroupedByShareIds(shareIds));

        List<AdminShareListItemResponse> records = sharePage.getContent().stream()
                .map(share -> AdminShareListItemResponse.fromEntity(
                        share,
                        likeCountMap.getOrDefault(share.getId(), 0L),
                        replyCountMap.getOrDefault(share.getId(), 0L)
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
    @Transactional(readOnly = true)
    public AdminShareDetailResponse getShareDetail(Long shareId) {
        POIShare share = poiShareRepository.findWithPoiUserAndImagesById(shareId)
                .orElseThrow(() -> new BusinessException(404, "分享不存在"));

        List<POIShareReplyResponse> replies = poiShareReplyRepository.findAllByShareIdOrderByCreatedAtAscIdAsc(shareId).stream()
                .map(reply -> POIShareReplyResponse.fromEntity(reply, null, UserRole.SUPER_ADMIN.getCode()))
                .toList();

        return AdminShareDetailResponse.fromEntity(
                share,
                poiShareLikeRepository.countByShareId(shareId),
                (long) replies.size(),
                replies
        );
    }

    @Override
    @Transactional
    public void deleteShare(Long shareId, Long operatorUserId) {
        String summary = poiShareRepository.findById(shareId)
                .map(share -> "删除分享 #" + share.getId() + "：" + buildPreview(share.getContent()))
                .orElse("删除分享 #" + shareId);
        poiShareService.deleteShare(shareId, operatorUserId);
        adminOperationLogService.record(operatorUserId, "内容管理", "删除分享", "分享", shareId, summary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReplyListItemResponse> getReplies(
            String keyword,
            Long shareId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer page,
            Integer size
    ) {
        validateTimeRange(startTime, endTime);
        validateShareIfPresent(shareId);

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<POIShareReply> replyPage = poiShareReplyRepository.findAll(buildReplySpecification(keyword, shareId, startTime, endTime), pageable);
        List<AdminReplyListItemResponse> records = replyPage.getContent().stream()
                .map(AdminReplyListItemResponse::fromEntity)
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
    public void deleteReply(Long replyId, Long operatorUserId) {
        String summary = poiShareReplyRepository.findById(replyId)
                .map(reply -> "删除回复 #" + reply.getId() + "：" + buildPreview(reply.getContent()))
                .orElse("删除回复 #" + replyId);
        poiShareService.deleteReply(replyId, operatorUserId);
        adminOperationLogService.record(operatorUserId, "内容管理", "删除回复", "回复", replyId, summary);
    }

    private Specification<POIShare> buildShareSpecification(
            String keyword,
            Long poiId,
            LocalDateTime startTime,
            LocalDateTime endTime
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

            if (startTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }

            if (endTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<POIShareReply> buildReplySpecification(
            String keyword,
            Long shareId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> userJoin = root.join("user");
            Join<Object, Object> shareJoin = root.join("share");

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("content"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(userJoin.get("displayName"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(shareJoin.get("content"), "")), normalizedKeyword)
                ));
            }

            if (shareId != null) {
                predicates.add(criteriaBuilder.equal(shareJoin.get("id"), shareId));
            }

            if (startTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }

            if (endTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(400, "开始时间不能晚于结束时间");
        }
    }

    private void validatePoiIfPresent(Long poiId) {
        if (poiId != null && !poiRepository.existsById(poiId)) {
            throw new BusinessException(404, "POI不存在");
        }
    }

    private void validateShareIfPresent(Long shareId) {
        if (shareId != null && !poiShareRepository.existsById(shareId)) {
            throw new BusinessException(404, "分享不存在");
        }
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "无文本内容";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 36) {
            return normalized;
        }
        return normalized.substring(0, 36) + "...";
    }
}
