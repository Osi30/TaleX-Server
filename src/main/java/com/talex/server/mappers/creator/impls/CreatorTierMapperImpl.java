package com.talex.server.mappers.creator.impls;

import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.mappers.creator.ICreatorTierMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CreatorTierMapperImpl implements ICreatorTierMapper {

    @Override
    public CreatorTierResponseDto toResponseDto(CreatorTier entity) {
        if (entity == null) {
            return null;
        }

        return CreatorTierResponseDto.builder()
                .creatorTierId(entity.getCreatorTierId())
                .tierName(entity.getTierName())
                .tierLevel(entity.getTierLevel())
                .minFollowerRequired(entity.getMinFollowerRequired())
                .minViewsRequired(entity.getMinViewsRequired())
                .minWatchTimeRequired(entity.getMinWatchTimeRequired())
                .premiumFundShareRatio(entity.getPremiumFundShareRatio())
                .directPurchaseShareRatio(entity.getDirectPurchaseShareRatio())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public CreatorTier toEntity(CreatorTierRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return CreatorTier.builder()
                .tierName(dto.getTierName())
                .tierLevel(dto.getTierLevel() != null ? dto.getTierLevel() : -1)
                .minFollowerRequired(dto.getMinFollowerRequired() != null ? dto.getMinFollowerRequired() : 0L)
                .minViewsRequired(dto.getMinViewsRequired() != null ? dto.getMinViewsRequired() : 0L)
                .minWatchTimeRequired(dto.getMinWatchTimeRequired() != null ? dto.getMinWatchTimeRequired() : 0.0)
                .premiumFundShareRatio(dto.getPremiumFundShareRatio())
                .directPurchaseShareRatio(dto.getDirectPurchaseShareRatio())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .isDeleted(false)
                .build();
    }

    @Override
    public void updateEntity(CreatorTierRequestDto dto, CreatorTier entity) {
        if (dto == null || entity == null) {
            return;
        }

        Optional.ofNullable(dto.getTierName()).ifPresent(entity::setTierName);
        Optional.ofNullable(dto.getTierLevel()).ifPresent(entity::setTierLevel);
        Optional.ofNullable(dto.getMinFollowerRequired()).ifPresent(entity::setMinFollowerRequired);
        Optional.ofNullable(dto.getMinViewsRequired()).ifPresent(entity::setMinViewsRequired);
        Optional.ofNullable(dto.getMinWatchTimeRequired()).ifPresent(entity::setMinWatchTimeRequired);
        Optional.ofNullable(dto.getPremiumFundShareRatio()).ifPresent(entity::setPremiumFundShareRatio);
        Optional.ofNullable(dto.getDirectPurchaseShareRatio()).ifPresent(entity::setDirectPurchaseShareRatio);
        Optional.ofNullable(dto.getIsDefault()).ifPresent(entity::setIsDefault);
    }
}
