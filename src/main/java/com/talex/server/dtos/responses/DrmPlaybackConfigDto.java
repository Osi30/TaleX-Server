package com.talex.server.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrmPlaybackConfigDto {
    private String widevineLicenseUrl;
    private String fairplayLicenseUrl;
    private String playreadyLicenseUrl;
    private String certificateUrl;
}
