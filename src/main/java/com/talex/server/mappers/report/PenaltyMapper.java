package com.talex.server.mappers.report;

import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.entities.report.Penalty;

public interface PenaltyMapper {
    PenaltyResponseDto toResponseDto(Penalty entity);
}
