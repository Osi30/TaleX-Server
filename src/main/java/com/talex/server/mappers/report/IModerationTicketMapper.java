package com.talex.server.mappers.report;


import com.talex.server.dtos.report.response.TicketResponseDto;
import com.talex.server.entities.report.ModerationTicket;

public interface IModerationTicketMapper {
    TicketResponseDto toResponseDto(ModerationTicket entity);
}