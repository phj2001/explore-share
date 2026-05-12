package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.POIFavoriteResponse;
import com.smartcampus.dto.response.POIFavoriteStatusResponse;
import com.smartcampus.service.POIFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class POIFavoriteController {

    private final POIFavoriteService poiFavoriteService;

    @GetMapping("/api/pois/{poiId}/favorite")
    public Result<POIFavoriteStatusResponse> getStatus(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiFavoriteService.getStatus(poiId, getOptionalUserId(authentication)));
    }

    @PostMapping("/api/pois/{poiId}/favorite")
    public Result<POIFavoriteStatusResponse> addFavorite(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiFavoriteService.addFavorite(poiId, getRequiredUserId(authentication)));
    }

    @DeleteMapping("/api/pois/{poiId}/favorite")
    public Result<POIFavoriteStatusResponse> removeFavorite(@PathVariable Long poiId, Authentication authentication) {
        return Result.success(poiFavoriteService.removeFavorite(poiId, getRequiredUserId(authentication)));
    }

    @GetMapping("/api/users/me/favorites")
    public Result<PageResponse<POIFavoriteResponse>> getUserFavorites(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(poiFavoriteService.getUserFavorites(getRequiredUserId(authentication), page, size));
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
