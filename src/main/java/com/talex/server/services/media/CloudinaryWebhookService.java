package com.talex.server.services.media;

import com.talex.server.dtos.responses.media.CloudinaryWebhookResponseDto;

public interface CloudinaryWebhookService {
    CloudinaryWebhookResponseDto handleNotification(String payload, String signature, String timestamp);
}
