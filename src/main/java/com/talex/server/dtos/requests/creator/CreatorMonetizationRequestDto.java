package com.talex.server.dtos.requests.creator;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorMonetizationRequestDto {
    @NotBlank(message = "Điều khoản không được để trống")
    private String termsId;
}
