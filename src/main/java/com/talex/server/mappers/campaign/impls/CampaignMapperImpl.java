package com.talex.server.mappers.campaign.impls;

import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.mappers.campaign.CampaignMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapperImpl implements CampaignMapper {

    @Override
    public CampaignResponseDto toResponseDto(Campaign entity) {
        if (entity == null) {
            return null;
        }

        return CampaignResponseDto.builder()
                .campaignId(entity.getCampaignId())
                .status(entity.getCampaignStatus())
                .targetImpression(entity.getTargetImpression())
                .currentImpression(entity.getCurrentImpression())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .orderId(entity.getOrderId())
                .engagementServiceId(
                        entity.getEngagementService() != null ? entity.getEngagementService().getEngagementServiceId()
                                : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .analyticData(entity.getAnalyticData())
                .build();
    }
}
