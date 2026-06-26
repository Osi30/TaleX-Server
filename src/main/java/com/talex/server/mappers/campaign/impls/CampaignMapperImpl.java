package com.talex.server.mappers.campaign.impls;

import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.mappers.campaign.ICampaignMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapperImpl implements ICampaignMapper {

    @Override
    public Campaign toEntity(CampaignRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return Campaign.builder()
                .status(requestDto.getStatus())
                .startAt(requestDto.getStartAt())
                .endAt(requestDto.getEndAt())
                .build();
    }

    @Override
    public CampaignResponseDto toResponseDto(Campaign entity) {
        if (entity == null) {
            return null;
        }

        return CampaignResponseDto.builder()
                .campaignId(entity.getCampaignId())
                .status(entity.getStatus())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .creatorId(entity.getCreator() != null ? entity.getCreator().getCreatorId() : null)
                .engagementServiceId(
                        entity.getEngagementService() != null ? entity.getEngagementService().getEngagementServiceId()
                                : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public void updateEntity(CampaignRequestDto requestDto, Campaign entity) {
        if (requestDto == null || entity == null) {
            return;
        }

        if (requestDto.getStatus() != null) {
            entity.setStatus(requestDto.getStatus());
        }
        if (requestDto.getStartAt() != null) {
            entity.setStartAt(requestDto.getStartAt());
        }
        if (requestDto.getEndAt() != null) {
            entity.setEndAt(requestDto.getEndAt());
        }
    }
}
