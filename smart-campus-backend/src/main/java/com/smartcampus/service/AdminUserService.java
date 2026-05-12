package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.AdminUserDetailResponse;
import com.smartcampus.dto.response.AdminUserListItemResponse;

public interface AdminUserService {

    PageResponse<AdminUserListItemResponse> getUsers(String keyword, Short role, Short status, Integer page, Integer size);

    AdminUserDetailResponse getUserDetail(Long userId);

    AdminUserDetailResponse updateUserRole(Long targetUserId, Short role, Boolean canResetPassword, Long operatorUserId);

    AdminUserDetailResponse updateUserStatus(Long targetUserId, Short status, Long operatorUserId);

    AdminUserDetailResponse updateCanResetPassword(Long targetUserId, Boolean canResetPassword, Long operatorUserId);

    void resetUserPassword(Long targetUserId, String newPassword, Long operatorUserId);
}
