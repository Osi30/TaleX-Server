package com.talex.server.repositories.report;

import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.enums.report.TargetType;
import com.talex.server.enums.report.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ModerationTicketRepository
        extends JpaRepository<ModerationTicket, String>, JpaSpecificationExecutor<ModerationTicket> {

    // Tìm Ticket đang mở hoặc đang xử lý cho 1 đối tượng cụ thể (để gộp Report mới vào)
    Optional<ModerationTicket> findByTargetTypeAndTargetIdAndStatusIn(
            TargetType targetType, String targetId, Collection<TicketStatus> statuses);

    Optional<ModerationTicket> findByTicketIdAndStatusIs(String ticketId, TicketStatus status);

    Optional<ModerationTicket> findByTicketIdAndStatusIsAndAssignedStaffId(String ticketId, TicketStatus status, String assignedStaffId);
}