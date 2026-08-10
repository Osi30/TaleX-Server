package com.talex.server.services.creator.impls;

import com.talex.server.dtos.requests.creator.CreatorConfigRequestDto;
import com.talex.server.dtos.responses.creator.CreatorConfigResponseDto;
import com.talex.server.entities.creator.CreatorConfig;
import com.talex.server.repositories.creator.CreatorConfigRepository;
import com.talex.server.services.creator.CreatorConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatorConfigServiceImpl implements CreatorConfigService {

    private final CreatorConfigRepository creatorConfigRepository;

    @Override
    @Transactional
    public CreatorConfigResponseDto createConfig(CreatorConfigRequestDto dto) {
        if (creatorConfigRepository.count() > 0) {
            throw new IllegalStateException("CreatorConfig đã tồn tại. Chỉ được phép khởi tạo một lần duy nhất.");
        }

        CreatorConfig config = CreatorConfig.builder()
                .basePremiumShare(dto.getBasePremiumShare())
                .baseUnlockShare(dto.getBaseUnlockShare())
                .build();

        CreatorConfig saved = creatorConfigRepository.save(config);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public CreatorConfigResponseDto updateConfig(CreatorConfigRequestDto dto) {
        CreatorConfig config = getConfigEntity();
        config.setBasePremiumShare(dto.getBasePremiumShare());
        config.setBaseUnlockShare(dto.getBaseUnlockShare());

        CreatorConfig updated = creatorConfigRepository.save(config);
        return mapToResponseDto(updated);
    }

    @Override
    public CreatorConfigResponseDto getConfigDto() {
        return mapToResponseDto(getConfigEntity());
    }

    @Override
    public CreatorConfig getConfigEntity() {
        return creatorConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CreatorConfig chưa được khởi tạo trong hệ thống."));
    }

    private CreatorConfigResponseDto mapToResponseDto(CreatorConfig config) {
        return CreatorConfigResponseDto.builder()
                .id(config.getId())
                .basePremiumShare(config.getBasePremiumShare())
                .baseUnlockShare(config.getBaseUnlockShare())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}