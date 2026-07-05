package com.talex.server.dtos.requests.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEngagementOrderRequestDto {
    @NotBlank
    private String engagementServiceId;

    @NotEmpty
    private List<String> episodeIds;
}
