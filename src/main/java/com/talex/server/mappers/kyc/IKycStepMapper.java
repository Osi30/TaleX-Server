package com.talex.server.mappers.kyc;

import com.talex.server.dtos.requests.kyc.KycStepRequestDto;
import com.talex.server.dtos.responses.kyc.KycStepResponseDto;
import com.talex.server.entities.kyc.KycStep;

public interface IKycStepMapper {
    KycStepResponseDto toResponseDto(KycStep kycStep);

    KycStep toDefaultEntity(KycStepRequestDto requestDto);
}
