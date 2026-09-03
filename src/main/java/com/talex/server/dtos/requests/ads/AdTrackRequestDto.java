package com.talex.server.dtos.requests.ads;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdTrackRequestDto {
    @NotNull(message = "Campaign ID is required")
    private UUID campaignId;

    private String source; // "IN_VIDEO", "POPUP", "MISSION"

    private String deviceId;
}
