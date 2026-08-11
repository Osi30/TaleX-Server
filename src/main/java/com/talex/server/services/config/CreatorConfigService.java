package com.talex.server.services.config;

import com.talex.server.dtos.requests.creator.CreatorConfigRequestDto;
import com.talex.server.dtos.responses.creator.CreatorConfigResponseDto;
import com.talex.server.entities.config.CreatorConfig;

public interface CreatorConfigService {
    CreatorConfigResponseDto createConfig(CreatorConfigRequestDto dto);
    CreatorConfigResponseDto updateConfig(CreatorConfigRequestDto dto);
    CreatorConfigResponseDto getConfigDto();
    CreatorConfig getConfigEntity();
}