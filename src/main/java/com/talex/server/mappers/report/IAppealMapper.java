package com.talex.server.mappers.report;

import com.talex.server.dtos.report.response.AppealResponseDto;
import com.talex.server.entities.report.Appeal;

public interface IAppealMapper {
    AppealResponseDto toResponseDto(Appeal entity);
}
