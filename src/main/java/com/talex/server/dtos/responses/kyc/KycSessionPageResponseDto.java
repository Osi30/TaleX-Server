package com.talex.server.dtos.responses.kyc;

import com.talex.server.dtos.BasePageResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class KycSessionPageResponseDto extends BasePageResponse<KycSessionResponseDto> {
}
