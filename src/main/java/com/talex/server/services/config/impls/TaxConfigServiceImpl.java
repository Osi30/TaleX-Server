package com.talex.server.services.config.impls;

import com.talex.server.dtos.requests.TaxConfigRequestDto;
import com.talex.server.dtos.responses.TaxConfigResponseDto;
import com.talex.server.entities.config.TaxConfig;
import com.talex.server.repositories.TaxConfigRepository;
import com.talex.server.services.config.TaxConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxConfigServiceImpl implements TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;

    @Override
    @Transactional
    public TaxConfigResponseDto createTaxConfig(TaxConfigRequestDto dto) {
        if (taxConfigRepository.count() > 0) {
            throw new IllegalStateException("TaxConfig đã tồn tại trong hệ thống. Chỉ cho phép khởi tạo 1 lần!");
        }

        TaxConfig taxConfig = TaxConfig.builder()
                .vat(dto.getVat())
                .pit(dto.getPit())
                .minPitAmount(dto.getMinPitAmount())
                .build();

        TaxConfig saved = taxConfigRepository.save(taxConfig);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public TaxConfigResponseDto updateTaxConfig(TaxConfigRequestDto dto) {
        TaxConfig taxConfig = getTaxConfigEntity();
        taxConfig.setVat(dto.getVat());
        taxConfig.setPit(dto.getPit());
        taxConfig.setMinPitAmount(dto.getMinPitAmount());

        TaxConfig updated = taxConfigRepository.save(taxConfig);
        return mapToResponseDto(updated);
    }

    @Override
    public TaxConfigResponseDto getTaxConfigDto() {
        return mapToResponseDto(getTaxConfigEntity());
    }

    @Override
    public TaxConfig getTaxConfigEntity() {
        return taxConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cấu hình thuế (TaxConfig) chưa được khởi tạo!"));
    }

    private TaxConfigResponseDto mapToResponseDto(TaxConfig config) {

        return TaxConfigResponseDto.builder()
                .id(config.getId())
                .vat(config.getVat())
                .pit(config.getPit())
                .minPitAmount(config.getMinPitAmount())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}