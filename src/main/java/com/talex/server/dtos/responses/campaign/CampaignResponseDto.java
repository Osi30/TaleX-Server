package com.talex.server.dtos.responses.campaign;

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
public class CampaignResponseDto {
    private String campaignId;
    private String name;
    private String description;
    private CampaignStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long budget;
    private String accountId;
    private String creatorId;
    private String engagementServiceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
