package com.talex.server.dtos.responses.ads;

import com.talex.server.enums.ads.AdCampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AdCampaignResponseDto {
    private UUID campaignId;
    private UUID profileId;
    private UUID slotId;
    private String slotCodeName;
    private String name;
    private AdCampaignStatus status;
    private Long campaignBalance;
    private Long targetImpressions;
    private Long currentImpressions;
    private Long currentClicks;
    private Long totalBudget;
    private String adminNote;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    
    private List<String> labels;
    private List<AdCreativeResponseDto> creatives;
}
