package com.talex.server.dtos.responses.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyrightResponseDto {
    private String copyrightId;
    private String code;
    private String name;
    private String description;
    private String legalUrl;
    private Boolean isActive;
    private String permissions;
}
