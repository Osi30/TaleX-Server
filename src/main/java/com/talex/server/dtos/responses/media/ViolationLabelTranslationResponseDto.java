package com.talex.server.dtos.responses.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationLabelTranslationResponseDto {
    private UUID translationId;
    private String awsLabel;
    private String vietnameseText;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
