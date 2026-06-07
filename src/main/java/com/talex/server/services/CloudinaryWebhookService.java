package com.talex.server.services;

import com.talex.server.dtos.responses.CloudinaryWebhookResponseDto;

public interface CloudinaryWebhookService {
    CloudinaryWebhookResponseDto handleNotification(String payload, String signature, String timestamp);
}
