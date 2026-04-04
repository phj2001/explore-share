package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.POICheckInStatusResponse;
import com.smartcampus.service.POICheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pois/{poiId}/check-in")
@RequiredArgsConstructor
public class POICheckInController {

    private final POICheckInService poiCheckInService;

    @GetMapping
    public Result<POICheckInStatusResponse> getStatus(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiCheckInService.getStatus(poiId, getOptionalUserId(authentication)));
    }

    @PostMapping
    public Result<POICheckInStatusResponse> checkIn(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiCheckInService.checkIn(poiId, getRequiredUserId(authentication)));
    }

    @DeleteMapping
    public Result<POICheckInStatusResponse> cancelCheckIn(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiCheckInService.cancelCheckIn(poiId, getRequiredUserId(authentication)));
    }

    private Long getOptionalUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private Long getRequiredUserId(Authentication authentication) {
        Long userId = getOptionalUserId(authentication);
        if (userId == null) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        return userId;
    }
}
