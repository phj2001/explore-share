package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.AdminFileResourceListItemResponse;
import com.smartcampus.service.AdminFileResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminFileResourceController {

    private final AdminFileResourceService adminFileResourceService;

    @GetMapping
    public Result<PageResponse<AdminFileResourceListItemResponse>> getResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(adminFileResourceService.getResources(keyword, resourceType, status, page, size));
    }

    @DeleteMapping
    public Result<Void> deleteResource(
            @RequestParam String resourceType,
            @RequestParam String resourceUrl,
            @RequestParam(required = false) Long ownerId,
            Authentication authentication
    ) {
        adminFileResourceService.deleteResource(resourceType, resourceUrl, ownerId, getCurrentUserId(authentication));
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
