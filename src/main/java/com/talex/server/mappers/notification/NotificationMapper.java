package com.talex.server.mappers.notification;

import com.talex.server.dtos.notification.NotificationResponseDto;
import com.talex.server.entities.Notification;

public interface NotificationMapper {
    NotificationResponseDto toResponseDto(Notification entity);
}