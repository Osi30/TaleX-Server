package com.talex.server.mappers.campaign.impls;

import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.mappers.campaign.IEngagementServiceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EngagementServiceMapperImpl implements IEngagementServiceMapper {

    @Override
    public EngagementService toEntity(EngagementServiceRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return EngagementService.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .engagementTarget(requestDto.getEngagementTarget())
                .engagementType(requestDto.getEngagementType())
                .targetValue(requestDto.getTargetValue())
                .isActive(requestDto.getIsActive())
                .build();
    }

    @Override
    public EngagementServiceResponseDto toResponseDto(EngagementService entity) {
        if (entity == null) {
            return null;
        }

        return EngagementServiceResponseDto.builder()
                .engagementServiceId(entity.getEngagementServiceId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .isActive(entity.getIsActive())
                .engagementType(entity.getEngagementType())
                .targetValue(entity.getTargetValue())
                .engagementTarget(entity.getEngagementTarget())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public void updateEntity(EngagementServiceRequestDto requestDto, EngagementService entity) {
        if (requestDto == null || entity == null) {
            return;
        }
        Optional.ofNullable(requestDto.getName()).ifPresent(entity::setName);
        Optional.ofNullable(requestDto.getDescription()).ifPresent(entity::setDescription);
        Optional.ofNullable(requestDto.getPrice()).ifPresent(entity::setPrice);
        Optional.ofNullable(requestDto.getIsActive()).ifPresent(entity::setIsActive);
        Optional.ofNullable(requestDto.getEngagementType()).ifPresent(entity::setEngagementType);
        Optional.ofNullable(requestDto.getEngagementTarget()).ifPresent(entity::setEngagementTarget);
        Optional.ofNullable(requestDto.getTargetValue()).ifPresent(entity::setTargetValue);
    }
}
