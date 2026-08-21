package com.talex.server.services.config.impls;

import com.talex.server.dtos.requests.config.SettlementConfigRequestDto;
import com.talex.server.dtos.responses.config.SettlementConfigResponseDto;
import com.talex.server.entities.config.SettlementConfig;
import com.talex.server.repositories.config.SettlementConfigRepository;
import com.talex.server.services.config.SettlementConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementConfigServiceImpl implements SettlementConfigService {

    private final SettlementConfigRepository settlementConfigRepository;

    @Override
    @Transactional
    public SettlementConfigResponseDto createSettlementConfig(SettlementConfigRequestDto dto) {
        if (settlementConfigRepository.count() > 0) {
            throw new IllegalStateException("SettlementConfig đã tồn tại trong hệ thống. Chỉ cho phép khởi tạo 1 lần!");
        }

        SettlementConfig config = SettlementConfig.builder()
                .minBalanceThreshold(dto.getMinBalanceThreshold())
                .build();

        SettlementConfig saved = settlementConfigRepository.save(config);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public SettlementConfigResponseDto updateSettlementConfig(SettlementConfigRequestDto dto) {
        SettlementConfig config = getSettlementConfigEntity();
        config.setMinBalanceThreshold(dto.getMinBalanceThreshold());

        SettlementConfig updated = settlementConfigRepository.save(config);
        return mapToResponseDto(updated);
    }

    @Override
    public SettlementConfigResponseDto getSettlementConfigDto() {
        return mapToResponseDto(getSettlementConfigEntity());
    }

    @Override
    public SettlementConfig getSettlementConfigEntity() {
        return settlementConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cấu hình quyết toán (SettlementConfig) chưa được khởi tạo!"));
    }

    private SettlementConfigResponseDto mapToResponseDto(SettlementConfig config) {
        return SettlementConfigResponseDto.builder()
                .id(config.getId())
                .minBalanceThreshold(config.getMinBalanceThreshold())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}