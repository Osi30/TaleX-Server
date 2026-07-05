package com.talex.server.dtos.requests.creator;

import com.talex.server.enums.creator.PaymentProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProfileVerifiedDto {
    @NotBlank
    private String verifiedNote;

    @NotNull
    private PaymentProfileStatus status;
}
