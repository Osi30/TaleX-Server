package com.talex.server.dtos.requests.campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestDto {
    @NotBlank
    private String engagementServiceId;

    @NotNull
    private List<String> seriesIds;

    @JsonIgnore
    private UUID accountId;

    @NotBlank
    private String orderId;
}
