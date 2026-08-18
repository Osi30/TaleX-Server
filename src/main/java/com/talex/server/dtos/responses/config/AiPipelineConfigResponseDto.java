package com.talex.server.dtos.responses.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPipelineConfigResponseDto {
    private UUID configId;
    private Double fingerprintSimilarityThreshold;
    private Double fingerprintClusterThreshold;
    private Double rekognitionConfidenceThreshold;
    private Double rekognitionViolenceConfidenceThreshold;
    private Integer fingerprintImageTopK;
    private Integer fingerprintVideoTopK;
    private Integer fingerprintMinMatchSeconds;
    private Integer fingerprintMaxGapSeconds;
    private Integer fingerprintFps;
    private Integer fingerprintMaxFrames;
    private Integer fingerprintMaxFileSizeMb;
    private Integer rekognitionMaxFrames;
    private Double moderationFrameInterval;
}
