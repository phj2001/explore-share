package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.request.CreatePOIApplicationRequest;
import com.smartcampus.dto.response.POIApplicationResponse;
import com.smartcampus.service.POIApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class POIApplicationController {

    private final POIApplicationService poiApplicationService;

    @PostMapping("/api/poi-applications")
    public Result<POIApplicationResponse> submitApplication(
            @Valid @RequestBody CreatePOIApplicationRequest request,
            Authentication authentication) {
        Long userId = getRequiredUserId(authentication);
        return Result.success(poiApplicationService.submitApplication(userId, request));
    }

    @GetMapping("/api/poi-applications/my")
    public Result<PageResponse<POIApplicationResponse>> getMyApplications(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long userId = getRequiredUserId(authentication);
        return Result.success(poiApplicationService.getMyApplications(userId, page, size));
    }

    private Long getRequiredUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
