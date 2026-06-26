package com.talex.server.dtos.responses.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLogResponseDto {
    private String campaignLogId;
    private String campaignId;
    private String accountId;
    private String eventType;
    private String message;
    private LocalDateTime createdAt;
}
