package com.talex.server.dtos.responses.series;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentWarningCategoryResponseDto {
    private UUID categoryId;
    private String code;
    private String label;
    private Boolean isActive;
}
