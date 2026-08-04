package com.talex.server.dtos.notification;

import com.talex.server.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private String notificationId;
    private String recipientId;
    private String title;
    private String content;
    private NotificationType type;
    private String referenceType;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}