package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.AdminUpdateSystemConfigRequest;
import com.smartcampus.dto.response.AdminSystemConfigItemResponse;
import com.smartcampus.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<AdminSystemConfigItemResponse>> getConfigs() {
        return Result.success(systemConfigService.getAdminConfigs());
    }

    @PutMapping("/{configKey:.+}")
    public Result<AdminSystemConfigItemResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody AdminUpdateSystemConfigRequest request,
            Authentication authentication
    ) {
        return Result.success(systemConfigService.updateConfig(configKey, request.getValue(), getCurrentUserId(authentication)));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
