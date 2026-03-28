package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminContentReportDetailResponse;
import com.smartcampus.dto.response.AdminContentReportListItemResponse;
import com.smartcampus.entity.ContentReport;
import com.smartcampus.entity.POIShare;
import com.smartcampus.entity.POIShareReply;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.ContentReportRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.AdminContentReportService;
import com.smartcampus.service.POIShareService;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminContentReportServiceImpl implements AdminContentReportService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContentReportRepository contentReportRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final UserRepository userRepository;
    private final POIShareService poiShareService;
    private final AdminOperationLogService adminOperationLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminContentReportListItemResponse> getReports(
            String keyword,
            Short targetType,
            Short status,
            Short reasonCode,
            Integer page,
            Integer size
    ) {
        validateTargetTypeIfPresent(targetType);
        validateStatusIfPresent(status);
        validateReasonCodeIfPresent(reasonCode);

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                pageNo,
                pageSize,
                Sort.by(Sort.Order.asc("status"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        Page<ContentReport> reportPage = contentReportRepository.findAll(buildSpecification(keyword, targetType, status, reasonCode), pageable);
        Map<String, Boolean> targetExistsMap = buildTargetExistsMap(reportPage.getContent());

        List<AdminContentReportListItemResponse> records = reportPage.getContent().stream()
                .map(report -> AdminContentReportListItemResponse.fromEntity(report, targetExistsMap.getOrDefault(buildTargetKey(report), false)))
                .toList();

        return new PageResponse<>(records, reportPage.getNumber(), reportPage.getSize(), reportPage.getTotalElements(), reportPage.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContentReportDetailResponse getReportDetail(Long reportId) {
        ContentReport report = getRequiredReport(reportId);
        boolean targetExists = isTargetExists(report.getTargetType(), report.getTargetId());
        String currentTargetContent = loadCurrentTargetContent(report.getTargetType(), report.getTargetId());
        return AdminContentReportDetailResponse.fromEntity(report, targetExists, currentTargetContent);
    }

    @Override
    @Transactional
    public AdminContentReportDetailResponse reviewReport(Long reportId, Short status, Short action, String reviewNote, Long operatorUserId) {
        ContentReport report = getRequiredReport(reportId);
        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        Short normalizedStatus = normalizeReviewStatus(status);
        Short normalizedAction = normalizeReviewAction(action, normalizedStatus);
        String normalizedNote = normalizeReviewNote(reviewNote);

        if (normalizedAction != null && normalizedAction == ContentReport.ACTION_DELETE_TARGET) {
            deleteTarget(report.getTargetType(), report.getTargetId(), operatorUserId);
        }

        report.setStatus(normalizedStatus);
        report.setReviewAction(normalizedAction);
        report.setReviewNote(normalizedNote);
        report.setReviewedBy(operator);
        report.setReviewedAt(LocalDateTime.now());

        ContentReport savedReport = contentReportRepository.save(report);
        adminOperationLogService.record(
                operatorUserId,
                "举报审核",
                "处理举报",
                "举报",
                savedReport.getId(),
                buildReviewSummary(savedReport)
        );
        boolean targetExists = isTargetExists(savedReport.getTargetType(), savedReport.getTargetId());
        String currentTargetContent = loadCurrentTargetContent(savedReport.getTargetType(), savedReport.getTargetId());
        return AdminContentReportDetailResponse.fromEntity(savedReport, targetExists, currentTargetContent);
    }

    private Specification<ContentReport> buildSpecification(String keyword, Short targetType, Short status, Short reasonCode) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("targetContentPreview")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("targetAuthorDisplayName")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("targetAuthorUsername")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("targetPoiName"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("reasonDetail"), "")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.join("reporter").get("username")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.join("reporter").get("displayName"), "")), normalizedKeyword)
                ));
            }

            if (targetType != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), targetType));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (reasonCode != null) {
                predicates.add(criteriaBuilder.equal(root.get("reasonCode"), reasonCode));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ContentReport getRequiredReport(Long reportId) {
        return contentReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(404, "举报记录不存在"));
    }

    private void validateTargetTypeIfPresent(Short targetType) {
        if (targetType == null) {
            return;
        }
        if (targetType != ContentReport.TARGET_TYPE_SHARE && targetType != ContentReport.TARGET_TYPE_REPLY) {
            throw new BusinessException(400, "不支持的举报对象类型");
        }
    }

    private void validateStatusIfPresent(Short status) {
        if (status == null) {
            return;
        }
        if (status < ContentReport.STATUS_PENDING || status > ContentReport.STATUS_REJECTED) {
            throw new BusinessException(400, "不支持的举报处理状态");
        }
    }

    private void validateReasonCodeIfPresent(Short reasonCode) {
        if (reasonCode == null) {
            return;
        }
        if (reasonCode < ContentReport.REASON_SPAM || reasonCode > ContentReport.REASON_OTHER) {
            throw new BusinessException(400, "不支持的举报理由");
        }
    }

    private Short normalizeReviewStatus(Short status) {
        if (status == null) {
            throw new BusinessException(400, "处理状态不能为空");
        }
        if (status != ContentReport.STATUS_PROCESSED && status != ContentReport.STATUS_REJECTED) {
            throw new BusinessException(400, "举报处理仅支持已处理或已驳回");
        }
        return status;
    }

    private Short normalizeReviewAction(Short action, Short status) {
        if (status == ContentReport.STATUS_REJECTED) {
            return ContentReport.ACTION_NONE;
        }
        if (action == null) {
            return ContentReport.ACTION_NONE;
        }
        if (action != ContentReport.ACTION_NONE && action != ContentReport.ACTION_DELETE_TARGET) {
            throw new BusinessException(400, "不支持的处理动作");
        }
        return action;
    }

    private String normalizeReviewNote(String reviewNote) {
        if (!StringUtils.hasText(reviewNote)) {
            return null;
        }
        String normalized = reviewNote.trim();
        if (normalized.length() > 200) {
            throw new BusinessException(400, "处理备注不能超过200个字符");
        }
        return normalized;
    }

    private void deleteTarget(Short targetType, Long targetId, Long operatorUserId) {
        if (targetType == ContentReport.TARGET_TYPE_SHARE) {
            poiShareService.deleteShare(targetId, operatorUserId);
            return;
        }
        if (targetType == ContentReport.TARGET_TYPE_REPLY) {
            poiShareService.deleteReply(targetId, operatorUserId);
            return;
        }
        throw new BusinessException(400, "不支持的举报对象类型");
    }

    private Map<String, Boolean> buildTargetExistsMap(List<ContentReport> reports) {
        List<Long> shareIds = reports.stream()
                .filter(report -> report.getTargetType() == ContentReport.TARGET_TYPE_SHARE)
                .map(ContentReport::getTargetId)
                .distinct()
                .toList();
        List<Long> replyIds = reports.stream()
                .filter(report -> report.getTargetType() == ContentReport.TARGET_TYPE_REPLY)
                .map(ContentReport::getTargetId)
                .distinct()
                .toList();

        Map<Long, Boolean> shareExists = poiShareRepository.findAllById(shareIds).stream()
                .collect(Collectors.toMap(POIShare::getId, share -> Boolean.TRUE));
        Map<Long, Boolean> replyExists = poiShareReplyRepository.findAllById(replyIds).stream()
                .collect(Collectors.toMap(POIShareReply::getId, reply -> Boolean.TRUE));

        return reports.stream().collect(Collectors.toMap(
                this::buildTargetKey,
                report -> report.getTargetType() == ContentReport.TARGET_TYPE_SHARE
                        ? shareExists.containsKey(report.getTargetId())
                        : replyExists.containsKey(report.getTargetId())
        ));
    }

    private boolean isTargetExists(Short targetType, Long targetId) {
        if (targetType == ContentReport.TARGET_TYPE_SHARE) {
            return poiShareRepository.existsById(targetId);
        }
        if (targetType == ContentReport.TARGET_TYPE_REPLY) {
            return poiShareReplyRepository.existsById(targetId);
        }
        return false;
    }

    private String loadCurrentTargetContent(Short targetType, Long targetId) {
        if (targetType == ContentReport.TARGET_TYPE_SHARE) {
            return poiShareRepository.findById(targetId).map(POIShare::getContent).orElse(null);
        }
        if (targetType == ContentReport.TARGET_TYPE_REPLY) {
            return poiShareReplyRepository.findById(targetId).map(POIShareReply::getContent).orElse(null);
        }
        return null;
    }

    private String buildTargetKey(ContentReport report) {
        return report.getTargetType() + ":" + report.getTargetId();
    }

    private String buildReviewSummary(ContentReport report) {
        String targetTypeLabel = report.getTargetType() == ContentReport.TARGET_TYPE_SHARE ? "分享" : "回复";
        if (report.getStatus() == ContentReport.STATUS_REJECTED) {
            return "驳回" + targetTypeLabel + "举报 #" + report.getId();
        }
        if (report.getReviewAction() != null && report.getReviewAction() == ContentReport.ACTION_DELETE_TARGET) {
            return "处理" + targetTypeLabel + "举报 #" + report.getId() + "，并删除被举报内容";
        }
        return "处理" + targetTypeLabel + "举报 #" + report.getId() + "，保留原内容";
    }
}
