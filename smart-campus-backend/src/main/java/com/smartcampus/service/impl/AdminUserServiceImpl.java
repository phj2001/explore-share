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
import com.smartcampus.service.AdminOperationLogService;
import com.smartcampus.service.AdminUserService;
import com.smartcampus.security.JwtTokenProvider;
import com.smartcampus.util.RedisUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final POIShareRepository poiShareRepository;
    private final POIShareReplyRepository poiShareReplyRepository;
    private final POIShareLikeRepository poiShareLikeRepository;
    private final AdminOperationLogService adminOperationLogService;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtils redisUtils;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItemResponse> getUsers(String keyword, Short role, Short status, Integer page, Integer size) {
        validateRoleIfPresent(role);
        validateStatusIfPresent(status);

        int pageNo = Math.max(page == null ? 0 : page, 0);
        int pageSize = Math.min(Math.max(size == null ? 10 : size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.unsorted());

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
    public AdminUserDetailResponse updateUserRole(Long targetUserId, Short role, Boolean canResetPassword, Long operatorUserId) {
        validateRole(role);
        User targetUser = getRequiredUser(targetUserId);
        User operatorUser = getRequiredUser(operatorUserId);

        if (targetUser.getRole() == UserRole.SUPER_ADMIN.getCode()) {
            throw new BusinessException(403, "不能修改超级管理员的角色");
        }

        if (operatorUser.getRole() != UserRole.SUPER_ADMIN.getCode()) {
            throw new BusinessException(403, "无权修改用户角色，该操作仅限超级管理员");
        }

        if (targetUserId.equals(operatorUserId) && UserRole.USER.getCode() == role) {
            throw new BusinessException(400, "不能将自己降级为普通用户");
        }

        targetUser.setRole(role);
        if (role == UserRole.ADMIN.getCode()) {
            // 晋升为管理员时，按参数决定是否授予重置密码权限
            targetUser.setCanResetPassword(Boolean.TRUE.equals(canResetPassword));
        } else {
            // 降级为普通用户时，清除重置密码权限
            targetUser.setCanResetPassword(false);
        }

        User savedUser = userRepository.save(targetUser);
        adminOperationLogService.record(
                operatorUserId,
                "用户管理",
                "更新用户角色",
                "用户",
                savedUser.getId(),
                "将用户 @" + savedUser.getUsername() + " 的角色更新为" + getRoleLabel(savedUser.getRole())
        );
        // 角色（如管理员降级为普通用户）已变更，立即清除用户信息缓存，避免最长 5 分钟的越权鉴权窗口期
        invalidateUserInfoCache(savedUser.getId());
        return buildUserDetail(savedUser);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateCanResetPassword(Long targetUserId, Boolean canResetPassword, Long operatorUserId) {
        User targetUser = getRequiredUser(targetUserId);
        User operatorUser = getRequiredUser(operatorUserId);

        if (operatorUser.getRole() != UserRole.SUPER_ADMIN.getCode()) {
            throw new BusinessException(403, "仅超级管理员可修改重置密码权限");
        }
        if (targetUser.getRole() != UserRole.ADMIN.getCode()) {
            throw new BusinessException(400, "仅管理员账号支持该操作");
        }

        targetUser.setCanResetPassword(Boolean.TRUE.equals(canResetPassword));
        User savedUser = userRepository.save(targetUser);
        adminOperationLogService.record(
                operatorUserId,
                "用户管理",
                "更新重置密码权限",
                "用户",
                savedUser.getId(),
                (Boolean.TRUE.equals(canResetPassword) ? "授予" : "撤销") + "用户 @" + savedUser.getUsername() + " 的重置密码权限"
        );
        return buildUserDetail(savedUser);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long targetUserId, String newPassword, Long operatorUserId) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new BusinessException(400, "新密码长度须在 6~64 位之间");
        }

        User targetUser = getRequiredUser(targetUserId);
        User operatorUser = getRequiredUser(operatorUserId);

        if (targetUser.getRole() == UserRole.SUPER_ADMIN.getCode()) {
            throw new BusinessException(403, "不能重置超级管理员的密码");
        }

        boolean isSuperAdmin = operatorUser.getRole() == UserRole.SUPER_ADMIN.getCode();
        boolean isAdminWithPermission = operatorUser.getRole() == UserRole.ADMIN.getCode()
                && Boolean.TRUE.equals(operatorUser.getCanResetPassword());

        if (!isSuperAdmin && !isAdminWithPermission) {
            throw new BusinessException(403, "无权重置用户密码");
        }

        if (isAdminWithPermission && targetUser.getRole() != UserRole.USER.getCode()) {
            throw new BusinessException(403, "管理员只能重置普通用户的密码");
        }

        targetUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);
        adminOperationLogService.record(
                operatorUserId,
                "用户管理",
                "重置用户密码",
                "用户",
                targetUserId,
                "重置了用户 @" + targetUser.getUsername() + " 的密码"
        );
        // 强制该用户下线：吊销所有旧 token + 清除用户信息缓存（与 changePassword/logout 一致的安全闭环），
        // 防止被重置密码的账号（或窃取其 token 的人）继续使用旧凭据
        forceInvalidateUserTokens(targetUserId);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserStatus(Long targetUserId, Short status, Long operatorUserId) {
        validateStatus(status);
        User targetUser = getRequiredUser(targetUserId);
        User operatorUser = getRequiredUser(operatorUserId);

        if (targetUser.getRole() == UserRole.SUPER_ADMIN.getCode()) {
            throw new BusinessException(403, "不能修改超级管理员的账号状态");
        }

        // 已注销账号是用户自主注销的终态，管理端不可再变更其状态
        if (UserStatus.fromCode(targetUser.getStatus()) == UserStatus.CANCELLED) {
            throw new BusinessException(400, "该用户已注销，无法修改账号状态");
        }

        // 注销只能由用户本人通过自主注销流程发起，管理端不提供代注销
        if (UserStatus.CANCELLED.getCode() == status) {
            throw new BusinessException(400, "不支持将用户设置为已注销状态");
        }

        if (operatorUser.getRole() == UserRole.ADMIN.getCode()
                && targetUser.getRole() != UserRole.USER.getCode()) {
            throw new BusinessException(403, "管理员只能修改普通用户的账号状态");
        }

        if (targetUserId.equals(operatorUserId) && UserStatus.DISABLED.getCode() == status) {
            throw new BusinessException(400, "不能禁用当前登录账号");
        }

        targetUser.setStatus(status);
        User savedUser = userRepository.save(targetUser);
        adminOperationLogService.record(
                operatorUserId,
                "用户管理",
                "更新用户状态",
                "用户",
                savedUser.getId(),
                "将用户 @" + savedUser.getUsername() + " 的账号状态更新为" + (savedUser.getStatus() == UserStatus.ACTIVE.getCode() ? "正常" : "禁用")
        );
        // 账号状态（如禁用）已变更，立即清除用户信息缓存；若被禁用，额外吊销所有旧 token 以强制立即下线
        invalidateUserInfoCache(savedUser.getId());
        if (UserStatus.fromCode(savedUser.getStatus()) == UserStatus.DISABLED) {
            forceInvalidateUserTokens(savedUser.getId());
        }
        return buildUserDetail(savedUser);
    }

    /** 清除用户信息缓存（user:info:{userId}），使下一次鉴权重新从数据库读取最新的 status/role。 */
    private void invalidateUserInfoCache(Long userId) {
        redisUtils.delete("user:info:" + userId);
    }

    /** 吊销该用户所有旧 token 并清除用户信息缓存，强制重新登录（复用 changePassword/logout 的安全闭环）。 */
    private void forceInvalidateUserTokens(Long userId) {
        long ttlSeconds = jwtTokenProvider.getExpirationMs() / 1000 + 60;
        redisUtils.set(
                "jwt:revoke_before:" + userId,
                String.valueOf(System.currentTimeMillis()),
                ttlSeconds,
                TimeUnit.SECONDS
        );
        redisUtils.delete("user:info:" + userId);
    }

    private String getRoleLabel(short role) {
        if (role == UserRole.SUPER_ADMIN.getCode()) return "超级管理员";
        if (role == UserRole.ADMIN.getCode()) return "管理员";
        return "普通用户";
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

            // 非 count 查询时加自定义排序：超级管理员(2)→管理员(3)→普通用户(1)，同级按 id 升序
            if (!Long.class.equals(query.getResultType())) {
                jakarta.persistence.criteria.Expression<Integer> roleOrder = criteriaBuilder
                        .<Integer>selectCase()
                        .when(criteriaBuilder.equal(root.<Short>get("role"), (short) UserRole.SUPER_ADMIN.getCode()), 0)
                        .when(criteriaBuilder.equal(root.<Short>get("role"), (short) UserRole.ADMIN.getCode()), 1)
                        .otherwise(2);
                query.orderBy(criteriaBuilder.asc(roleOrder), criteriaBuilder.asc(root.get("id")));
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
        return UserRole.USER.getCode() == role
                || UserRole.ADMIN.getCode() == role;
    }
}
