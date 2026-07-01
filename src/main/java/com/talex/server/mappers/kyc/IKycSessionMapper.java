package com.talex.server.mappers.kyc;

import com.talex.server.dtos.responses.kyc.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.kyc.KycSessionResponseDto;
import com.talex.server.entities.kyc.KycSession;
import org.springframework.data.domain.Page;

public interface IKycSessionMapper {
    KycSessionResponseDto toResponseDto(KycSession kycSession);

    KycSessionPageResponseDto toPageResponseDto(Page<KycSessionResponseDto> page);
}
