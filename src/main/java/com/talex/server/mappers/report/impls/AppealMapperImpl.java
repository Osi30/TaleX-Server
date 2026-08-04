package com.talex.server.mappers.report.impls;

import com.talex.server.dtos.report.response.AppealResponseDto;
import com.talex.server.entities.report.Appeal;
import com.talex.server.mappers.report.IAppealMapper;
import com.talex.server.mappers.report.IPenaltyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppealMapperImpl implements IAppealMapper {
    private final IPenaltyMapper penaltyMapper;

    @Override
    public AppealResponseDto toResponseDto(Appeal entity) {
        if (entity == null) return null;
        return AppealResponseDto.builder()
                .appealId(entity.getAppealId())
                .penaltyId(entity.getPenalty() != null ? entity.getPenalty().getPenaltyId() : null)
                .appellantId(entity.getAppellantId())
                .reviewerId(entity.getReviewerId())
                .reason(entity.getReason())
                .proofDocuments(entity.getProofDocuments())
                .status(entity.getStatus())
                .adminNote(entity.getAdminNote())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .penalty(penaltyMapper.toResponseDto(entity.getPenalty()))
                .build();
    }
}