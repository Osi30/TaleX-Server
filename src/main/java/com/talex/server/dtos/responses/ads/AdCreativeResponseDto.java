package com.talex.server.dtos.responses.ads;

import com.talex.server.enums.ads.AdMediaType;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class AdCreativeResponseDto {
    private UUID creativeId;
    private AdMediaType mediaType;
    private String mediaUrl;
    private String targetUrl;
}
