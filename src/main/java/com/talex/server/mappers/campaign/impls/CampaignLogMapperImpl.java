package com.talex.server.mappers.campaign.impls;

import com.talex.server.dtos.requests.campaign.CampaignLogRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignLogResponseDto;
import com.talex.server.entities.campaign.CampaignLog;
import com.talex.server.mappers.campaign.ICampaignLogMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignLogMapperImpl implements ICampaignLogMapper {

    @Override
    public CampaignLog toEntity(CampaignLogRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return CampaignLog.builder()
                .build();
    }

    @Override
    public CampaignLogResponseDto toResponseDto(CampaignLog entity) {
        if (entity == null) {
            return null;
        }

        return CampaignLogResponseDto.builder()
                .campaignLogId(entity.getCampaignLogId())
                .campaignId(entity.getCampaign() != null ? entity.getCampaign().getCampaignId() : null)
                .build();
    }
}
