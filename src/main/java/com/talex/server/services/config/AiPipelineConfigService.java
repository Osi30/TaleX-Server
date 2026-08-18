package com.talex.server.services.config;

import com.talex.server.dtos.requests.config.AiPipelineConfigRequestDto;
import com.talex.server.dtos.responses.config.AiPipelineConfigResponseDto;

public interface AiPipelineConfigService {
    AiPipelineConfigResponseDto getConfig();
    AiPipelineConfigResponseDto updateConfig(AiPipelineConfigRequestDto request);
}
