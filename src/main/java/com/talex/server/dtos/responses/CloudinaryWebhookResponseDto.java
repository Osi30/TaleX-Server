package com.talex.server.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryWebhookResponseDto {
    private String status;
    private String action;
    private String publicId;
    private String mediaId;
}
