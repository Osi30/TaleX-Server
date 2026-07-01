package com.talex.server.dtos.responses.campaign;

import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.engagement.EngagementTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponseDto {
    private String campaignId;
    private String engagementServiceId;
    private String orderId;
    private CampaignStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long targetValue;
    private Long currentValue;
    private EngagementTarget engagementTarget;
}
