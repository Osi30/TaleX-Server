package com.talex.server.services.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedUploadParams {
    private String cloudName;
    private String apiKey;
    private Long timestamp;
    private String signature;
    private String uploadUrl;
    private String resourceType;
    private Map<String, String> uploadParams;
}
