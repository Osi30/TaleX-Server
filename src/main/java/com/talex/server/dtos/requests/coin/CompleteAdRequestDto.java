package com.talex.server.dtos.requests.coin;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload used to complete and validate a rewarded ad session.")
public class CompleteAdRequestDto {

    @Schema(
            description = "Secure ad session ID returned by the start endpoint.",
            example = "6f7f9306-4f2f-43e2-b42d-47a7a4fd15ff",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Session ID must not be blank")
    private String sessionId;
}
