package com.smartcampus.controller;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.common.Result;
import com.smartcampus.dto.response.NotificationResponse;
import com.smartcampus.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public Result<PageResponse<NotificationResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long userId = getRequiredUserId(authentication);
        return Result.success(notificationService.getNotifications(userId, page, size));
    }

    @GetMapping("/api/notifications/unread-count")
    public Result<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = getRequiredUserId(authentication);
        return Result.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PostMapping("/api/notifications/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(id, getRequiredUserId(authentication));
        return Result.success(null);
    }

    @PostMapping("/api/notifications/read-all")
    public Result<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(getRequiredUserId(authentication));
        return Result.success(null);
    }

    private Long getRequiredUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }
}
