package com.talex.server.services;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.notification.NotificationResponseDto;

public interface NotificationService {
    BasePageResponse<NotificationResponseDto> getMyNotifications(String recipientId, BaseFilterRequestDto filterRequest);
    long getUnreadCount(String recipientId);
    void markAsRead(String notificationId, String recipientId);
    void markAllAsRead(String recipientId);
    int deleteReadNotifications(String recipientId);
}
