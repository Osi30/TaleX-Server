package com.talex.server.dtos.requests.campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestDto {
    @NotBlank
    private String episodeId;

    @NotBlank
    private String engagementServiceId;

    @JsonIgnore
    private UUID accountId;
}
