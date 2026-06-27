package com.smartcampus.controller;

import com.smartcampus.dto.common.Result;
import com.smartcampus.entity.NotificationEventOutbox;
import com.smartcampus.repository.NotificationEventOutboxRepository;
import com.smartcampus.service.NotificationOutboxDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知 Outbox 管理接口（升级项④）
 *
 * <p>供后台管理员查看 outbox 投递总览、手动重试失败事件。
 */
@RestController
@RequestMapping("/api/admin/notification-outbox")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminNotificationOutboxController {

    private final NotificationEventOutboxRepository outboxRepository;
    private final NotificationOutboxDispatcherService dispatcherService;

    /** 投递总览：待发/成功/失败计数 */
    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        long pending = outboxRepository.countByDeliveryStatus(NotificationEventOutbox.STATUS_PENDING);
        long success = outboxRepository.countByDeliveryStatus(NotificationEventOutbox.STATUS_SUCCESS);
        long failed = outboxRepository.countByDeliveryStatus(NotificationEventOutbox.STATUS_FAILED);
        return Result.success(Map.of("pending", pending, "success", success, "failed", failed));
    }

    /** 手动重试单条 */
    @PostMapping("/retry/{id}")
    public Result<Boolean> retryOne(@PathVariable Long id) {
        return Result.success(dispatcherService.retryEvent(id));
    }

    /** 手动重试所有失败事件 */
    @PostMapping("/retry-failed")
    public Result<Integer> retryFailed() {
        return Result.success(dispatcherService.retryFailedEvents());
    }
}
