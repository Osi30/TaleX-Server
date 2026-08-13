package com.talex.server.repositories.transaction;

import com.talex.server.entities.creator.PayoutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, String> {
}
