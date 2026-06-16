package com.talex.server.dtos.requests.kyc;

import com.talex.server.enums.kyc.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycSessionRequestDto {
    private KycStatus status;
}
