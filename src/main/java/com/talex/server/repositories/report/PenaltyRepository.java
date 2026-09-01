package com.talex.server.repositories.report;

import com.talex.server.entities.report.Penalty;
import com.talex.server.enums.report.PenaltyLevel;
import com.talex.server.enums.report.PenaltyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltyRepository
        extends JpaRepository<Penalty, String>, JpaSpecificationExecutor<Penalty> {

    Page<Penalty> findByTargetUserId(String targetUserId, Pageable pageable);

    List<Penalty> findByTicketId(String ticketId);

    Optional<Penalty> findByPenaltyIdAndStatus(String penaltyId, PenaltyStatus status);
}