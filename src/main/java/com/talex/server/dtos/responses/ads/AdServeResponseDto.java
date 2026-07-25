package com.talex.server.dtos.responses.ads;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class AdServeResponseDto {
    private UUID campaignId;
    private String mediaUrl;
    private String targetUrl;
    private String mediaType; // IMAGE, VIDEO
}
