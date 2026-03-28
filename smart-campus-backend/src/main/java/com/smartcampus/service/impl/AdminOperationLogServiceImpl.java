package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminOperationLogListItemResponse;
import com.smartcampus.entity.AdminOperationLog;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.AdminOperationLogRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.service.AdminOperationLogService;
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
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminOperationLogRepository adminOperationLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(Long operatorUserId, String moduleName, String actionName, String targetType, Long targetId, String summary) {
        if (operatorUserId == null) {
            return;
        }

        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new BusinessException(404, "管理员不存在"));

        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorUserId(operator.getId());
        log.setOperatorDisplayName(StringUtils.hasText(operator.getDisplayName()) ? operator.getDisplayName() : operator.getUsername());
        log.setOperatorUsername(operator.getUsername());
        log.setModuleName(trimToLength(moduleName, 50));
        log.setActionName(trimToLength(actionName, 80));
        log.setTargetType(trimToLength(targetType, 50));
        log.setTargetId(targetId);
        log.setSummary(trimToLength(summary, 300));
        adminOperationLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOperationLogListItemResponse> getLogs(String keyword, String moduleName, Integer page, Integer size) {
        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<AdminOperationLog> result = adminOperationLogRepository.findAll(buildSpecification(keyword, moduleName), pageable);
        List<AdminOperationLogListItemResponse> records = result.getContent().stream()
                .map(AdminOperationLogListItemResponse::fromEntity)
                .toList();

        return new PageResponse<>(records, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private Specification<AdminOperationLog> buildSpecification(String keyword, String moduleName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String normalized = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("operatorUsername")), normalized),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("operatorDisplayName"), "")), normalized),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("moduleName")), normalized),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("actionName")), normalized),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("targetType")), normalized),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalized)
                ));
            }

            if (StringUtils.hasText(moduleName)) {
                predicates.add(criteriaBuilder.equal(root.get("moduleName"), moduleName.trim()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }
}
