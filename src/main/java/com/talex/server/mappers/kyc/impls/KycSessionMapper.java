package com.talex.server.mappers.kyc.impls;

import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.entities.kyc.KycSession;
import com.talex.server.mappers.kyc.IKycSessionMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class KycSessionMapper implements IKycSessionMapper {

    @Override
    public KycSessionResponseDto toResponseDto(KycSession kycSession) {
        if (kycSession == null)
            return null;

        return KycSessionResponseDto.builder()
                .kycSessionId(kycSession.getKycSessionId())
                .status(kycSession.getStatus())
                .startedAt(kycSession.getStartedAt())
                .completedAt(kycSession.getCompletedAt())
                .updatedAt(kycSession.getUpdatedAt())
                .build();
    }

    @Override
    public KycSessionPageResponseDto toPageResponseDto(Page<KycSessionResponseDto> page) {
        if (page == null)
            return null;

        return KycSessionPageResponseDto.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
