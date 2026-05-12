package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.AdminUpdateUserRoleRequest;
import com.smartcampus.dto.request.AdminUpdateUserStatusRequest;
import com.smartcampus.dto.response.AdminUserDetailResponse;
import com.smartcampus.dto.response.AdminUserListItemResponse;
import com.smartcampus.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<PageResponse<AdminUserListItemResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short role,
            @RequestParam(required = false) Short status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminUserService.getUsers(keyword, role, status, page, size));
    }

    @GetMapping("/{userId}")
    public Result<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return Result.success(adminUserService.getUserDetail(userId));
    }

    @PutMapping("/{userId}/role")
    public Result<AdminUserDetailResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRoleRequest request,
            Authentication authentication
    ) {
        return Result.success(adminUserService.updateUserRole(userId, request.getRole(), request.getCanResetPassword(), getCurrentUserId(authentication)));
    }

    @PutMapping("/{userId}/status")
    public Result<AdminUserDetailResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return Result.success(adminUserService.updateUserStatus(userId, request.getStatus(), getCurrentUserId(authentication)));
    }

    @PutMapping("/{userId}/can-reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<AdminUserDetailResponse> updateCanResetPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication
    ) {
        Boolean canReset = body.get("canResetPassword");
        return Result.success(adminUserService.updateCanResetPassword(userId, canReset, getCurrentUserId(authentication)));
    }

    @PutMapping("/{userId}/password")
    public Result<Void> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        adminUserService.resetUserPassword(userId, body.get("newPassword"), getCurrentUserId(authentication));
        return Result.success(null);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
