package com.talex.server.dtos.responses.coin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Secure rewarded ad session metadata returned to the client.")
public class AdSessionResponseDto {

    @Schema(
            description = "Secure session ID that must be submitted after the ad is watched.",
            example = "6f7f9306-4f2f-43e2-b42d-47a7a4fd15ff"
    )
    private String sessionId;

    @Schema(description = "Number of seconds before the ad session expires.", example = "90")
    private int expiresInSeconds;
}
