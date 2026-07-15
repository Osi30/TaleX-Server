package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.media.CloudinaryWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CloudinaryWebhookController {
    private final CloudinaryWebhookService cloudinaryWebhookService;

    @PostMapping("/api/v1/webhooks/cloudinary")
    public ResponseEntity<BaseResponse> handleCloudinaryWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Cld-Signature", required = false) String signature,
            @RequestHeader(value = "X-Cld-Timestamp", required = false) String timestamp) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cloudinary webhook accepted")
                .data(cloudinaryWebhookService.handleNotification(payload, signature, timestamp))
                .build());
    }
}
