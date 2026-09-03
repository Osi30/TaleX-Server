package com.talex.server.dtos.responses.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdServeResponseDto {
    private UUID campaignId;
    private String mediaUrl;
    private String targetUrl;
    private String mediaType; // IMAGE, VIDEO
}
