package com.smartcampus.service.impl;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminUserDetailResponse;
import com.smartcampus.dto.response.AdminUserListItemResponse;
import com.smartcampus.entity.User;
import com.smartcampus.exception.BusinessException;
import com.smartcampus.repository.POIShareLikeRepository;
import com.smartcampus.repository.POIShareReplyRepository;
import com.smartcampus.repository.POIShareRepository;
import com.smartcampus.repository.UserRepository;
import com.smartcampus.security.UserRole;
import com.smartcampus.security.UserStatus;
import com.smartcampus.service.AdminUserService;
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
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIShareLikeRepository poiShareLikeRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItemResponse> getUsers(String keyword, Short role, Short status, Integer page, Integer size) {
        validateRoleIfPresent(role);
        validateStatusIfPresent(status);

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<User> userPage = userRepository.findAll(buildSpecification(keyword, role, status), pageable);
        List<AdminUserListItemResponse> records = userPage.getContent().stream()
                .map(AdminUserListItemResponse::fromUser)
                .toList();

        return new PageResponse<>(
                records,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = getRequiredUser(userId);
        return buildUserDetail(user);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserRole(Long targetUserId, Short role, Long operatorUserId) {
        validateRole(role);
        User targetUser = getRequiredUser(targetUserId);

        if (targetUserId.equals(operatorUserId) && UserRole.USER.getCode() == role) {
            throw new BusinessException(400, "不能将自己降级为普通用户");
        }

        targetUser.setRole(role);
        User savedUser = userRepository.save(targetUser);
        return buildUserDetail(savedUser);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserStatus(Long targetUserId, Short status, Long operatorUserId) {
        validateStatus(status);
        User targetUser = getRequiredUser(targetUserId);

        if (targetUserId.equals(operatorUserId) && UserStatus.DISABLED.getCode() == status) {
            throw new BusinessException(400, "不能禁用当前登录账号");
        }

        targetUser.setStatus(status);
        User savedUser = userRepository.save(targetUser);
        return buildUserDetail(savedUser);
    }

    private Specification<User> buildSpecification(String keyword, Short role, Short status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("displayName"), "")), normalizedKeyword)
                ));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminUserDetailResponse buildUserDetail(User user) {
        return AdminUserDetailResponse.fromUser(
                user,
                poiShareRepository.countByUserId(user.getId()),
                poiShareReplyRepository.countByUserId(user.getId()),
                poiShareLikeRepository.countByUserId(user.getId())
        );
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private void validateRoleIfPresent(Short role) {
        if (role != null) {
            validateRole(role);
        }
    }

    private void validateStatusIfPresent(Short status) {
        if (status != null) {
            validateStatus(status);
        }
    }

    private void validateRole(Short role) {
        if (role == null || !isSupportedRole(role)) {
            throw new BusinessException(400, "不支持的用户角色");
        }
    }

    private void validateStatus(Short status) {
        if (status == null || !UserStatus.isValidCode(status)) {
            throw new BusinessException(400, "不支持的账号状态");
        }
    }

    private boolean isSupportedRole(Short role) {
        return UserRole.USER.getCode() == role || UserRole.SUPER_ADMIN.getCode() == role;
    }
}
