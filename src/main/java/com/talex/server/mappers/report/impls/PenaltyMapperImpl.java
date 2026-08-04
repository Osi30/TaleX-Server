package com.talex.server.mappers.report.impls;

import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.entities.report.Penalty;
import com.talex.server.mappers.report.IPenaltyMapper;
import org.springframework.stereotype.Component;

@Component
public class PenaltyMapperImpl implements IPenaltyMapper {

    @Override
    public PenaltyResponseDto toResponseDto(Penalty entity) {
        if (entity == null) return null;
        return PenaltyResponseDto.builder()
                .penaltyId(entity.getPenaltyId())
                .ticketId(entity.getTicketId())
                .targetUserId(entity.getTargetUserId())
                .issuerId(entity.getIssuerId())
                .level(entity.getLevel())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}