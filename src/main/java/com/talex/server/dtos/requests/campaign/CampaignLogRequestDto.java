package com.talex.server.dtos.requests.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLogRequestDto {
    private String campaignId;
    private String accountId;
    private String eventType;
    private String message;
}
