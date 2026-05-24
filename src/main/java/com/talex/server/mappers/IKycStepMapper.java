package com.talex.server.mappers;

import com.talex.server.dtos.requests.KycStepRequestDto;
import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.entities.KycStep;

public interface IKycStepMapper {
    KycStepResponseDto toResponseDto(KycStep kycStep);

    KycStep toDefaultEntity(KycStepRequestDto requestDto);
}
