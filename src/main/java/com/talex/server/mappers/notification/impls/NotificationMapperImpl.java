package com.talex.server.mappers.notification.impls;

import com.talex.server.dtos.notification.NotificationResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.mappers.notification.INotificationMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapperImpl implements INotificationMapper {

    @Override
    public NotificationResponseDto toResponseDto(Notification entity) {
        if (entity == null) return null;
        return NotificationResponseDto.builder()
                .notificationId(entity.getNotificationId())
                .recipientId(entity.getRecipientId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}