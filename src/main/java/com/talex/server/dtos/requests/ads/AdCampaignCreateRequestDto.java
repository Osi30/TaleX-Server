package com.talex.server.dtos.requests.ads;

import com.talex.server.enums.ads.AdMediaType;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Data
public class AdCampaignCreateRequestDto {
    @NotNull(message = "Slot ID is required")
    private UUID slotId;

    @NotBlank(message = "Campaign name is required")
    private String name;

    @NotNull(message = "Target impressions is required")
    @Min(value = 1, message = "Target impressions must be at least 1")
    private Long targetImpressions;

    @NotNull(message = "Campaign budget is required")
    @Min(value = 10000, message = "Budget must be at least 10,000 VND")
    private Long campaignBudget;

    @NotNull(message = "Media type is required")
    private AdMediaType mediaType;

    @NotBlank(message = "Media URL is required")
    private String mediaUrl;

    @NotBlank(message = "Target URL is required")
    private String targetUrl;

    private java.util.List<String> labels;
}
