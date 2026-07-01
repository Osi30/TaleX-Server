package com.talex.server.mappers.kyc.impls;

import com.talex.server.dtos.requests.kyc.KycStepRequestDto;
import com.talex.server.dtos.responses.kyc.KycStepResponseDto;
import com.talex.server.entities.kyc.KycStep;
import com.talex.server.mappers.kyc.IKycStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycStepMapperImpl implements IKycStepMapper {

    @Override
    public KycStepResponseDto toResponseDto(KycStep kycStep) {
        if (kycStep == null) {
            return null;
        }

        return KycStepResponseDto.builder()
                .kycStepId(kycStep.getKycStepId())
                .stepType(kycStep.getStepType())
                .isSuccess(kycStep.getIsSuccess())
                .message(kycStep.getMessage())
                .provider(kycStep.getProvider())
                .rawResponse(kycStep.getRawResponse() != null ? kycStep.getRawResponse().toString() : null)
                .sessionId(kycStep.getKycSession() != null ? kycStep.getKycSession().getKycSessionId() : null)
                .createdAt(kycStep.getCreatedAt())
                .updatedAt(kycStep.getUpdatedAt())
                .build();
    }

    @Override
    public KycStep toDefaultEntity(KycStepRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return KycStep.builder()
                .stepType(requestDto.getStepType())
                .isSuccess(Boolean.FALSE)
                .message("")
                .provider(requestDto.getProvider())
                .build();
    }
}
