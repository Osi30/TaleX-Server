package com.talex.server.services;

import com.talex.server.dtos.requests.TaxConfigRequestDto;
import com.talex.server.dtos.responses.TaxConfigResponseDto;
import com.talex.server.entities.config.TaxConfig;

public interface TaxConfigService {
    TaxConfigResponseDto createTaxConfig(TaxConfigRequestDto dto);
    TaxConfigResponseDto updateTaxConfig(TaxConfigRequestDto dto);
    TaxConfigResponseDto getTaxConfigDto();
    TaxConfig getTaxConfigEntity();
}
