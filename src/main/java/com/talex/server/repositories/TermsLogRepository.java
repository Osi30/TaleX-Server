package com.talex.server.repositories;

import com.talex.server.entities.TermsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TermsLogRepository extends JpaRepository<TermsLog, String> {
    List<TermsLog> findByAccount_AccountId(UUID accountId);
}
