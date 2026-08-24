package com.talex.server.repositories.transaction;

import com.talex.server.entities.transaction.OrderInterventionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInterventionLogRepository extends JpaRepository<OrderInterventionLog, String> {
}
