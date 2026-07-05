package com.talex.server.dtos.requests.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContentOrderRequestDto {
    @NotBlank
    private String itemId;

    @NotBlank
    private String itemType;
}
