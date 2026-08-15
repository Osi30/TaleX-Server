package com.talex.server.dtos.campaign.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talex.server.enums.engagement.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignUpdateDto {
    private CampaignStatus status;

    @JsonIgnore
    private LocalDateTime endAt;

    @JsonIgnore
    private Long currentValue = 0L;
}
