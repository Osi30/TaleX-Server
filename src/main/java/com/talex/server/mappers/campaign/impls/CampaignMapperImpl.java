package com.talex.server.mappers.campaign.impls;

import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.mappers.campaign.ICampaignMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapperImpl implements ICampaignMapper {

    @Override
    public CampaignResponseDto toResponseDto(Campaign entity) {
        if (entity == null) {
            return null;
        }

        return CampaignResponseDto.builder()
                .campaignId(entity.getCampaignId())
                .status(entity.getStatus())
                .targetValue(entity.getTargetValue())
                .currentValue(entity.getCurrentValue())
                .engagementTarget(entity.getEngagementTarget())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .episodeId(entity.getEpisode() != null ? entity.getEpisode().getEpisodeId() : null)
                .engagementServiceId(
                        entity.getEngagementService() != null ? entity.getEngagementService().getEngagementServiceId()
                                : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
