package com.talex.server.dtos.requests.ads;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class AdTrackRequestDto {
    @NotNull(message = "Campaign ID is required")
    private UUID campaignId;
}
