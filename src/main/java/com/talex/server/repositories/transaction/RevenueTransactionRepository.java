package com.talex.server.repositories.transaction;

import com.talex.server.entities.creator.RevenueTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueTransactionRepository extends JpaRepository<RevenueTransaction, String> {
}