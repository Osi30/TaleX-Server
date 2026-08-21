package com.talex.server.services.config;

import com.talex.server.dtos.requests.config.SettlementConfigRequestDto;
import com.talex.server.dtos.responses.config.SettlementConfigResponseDto;
import com.talex.server.entities.config.SettlementConfig;

public interface SettlementConfigService {
    SettlementConfigResponseDto createSettlementConfig(SettlementConfigRequestDto dto);
    SettlementConfigResponseDto updateSettlementConfig(SettlementConfigRequestDto dto);
    SettlementConfigResponseDto getSettlementConfigDto();
    SettlementConfig getSettlementConfigEntity();
}