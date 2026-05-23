package com.talex.server.mappers;

import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.entities.KycSession;
import org.springframework.data.domain.Page;

public interface IKycSessionMapper {
    KycSessionResponseDto toResponseDto(KycSession kycSession);

    KycSessionPageResponseDto toPageResponseDto(Page<KycSessionResponseDto> page);
}
