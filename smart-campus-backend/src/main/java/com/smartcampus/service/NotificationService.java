package com.smartcampus.service;

import com.smartcampus.dto.common.PageResponse;
import com.smartcampus.dto.response.NotificationResponse;

public interface NotificationService {

    void sendNotification(Long recipientId, Long actorId, String type, String title, String content, String targetType, Long targetId);

    PageResponse<NotificationResponse> getNotifications(Long userId, Integer page, Integer size);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
