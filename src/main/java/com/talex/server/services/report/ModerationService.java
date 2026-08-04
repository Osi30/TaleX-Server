package com.talex.server.services.report;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.TicketProcessRequestDto;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.dtos.report.response.TicketResponseDto;

public interface ModerationService {
    BasePageResponse<TicketResponseDto> filterTickets(BaseFilterRequestDto filterRequest);
    TicketResponseDto assignTicketToStaff(String ticketId, String role, String staffId);
    PenaltyResponseDto processTicket(String ticketId, String staffId, String role, TicketProcessRequestDto requestDto);
}
