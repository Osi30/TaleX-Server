package com.talex.server.dtos.requests.coin;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload used to initialize a rewarded ad session for a mission.")
public class StartAdRequestDto {

    @Schema(
            description = "Business code of the mission that should receive progress after the ad is completed.",
            example = "WATCH_AD_DAILY",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Mission code must not be blank")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã nhiệm vụ chỉ được chứa chữ in hoa, số và dấu gạch dưới")
    private String missionCode;
}
