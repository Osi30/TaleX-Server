package com.talex.server.dtos.requests.ads;

import com.talex.server.enums.ads.AdCampaignStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AdCampaignReviewRequestDto {
    @NotNull(message = "Review status is required")
    private AdCampaignStatus status; // Usually ACTIVE or REJECTED

    private String adminNote;
}
