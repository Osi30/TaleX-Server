package com.talex.server.repositories.transaction;

import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByReferenceTypeAndReferenceIdIn(ReferenceType referenceType, List<String> referenceIds);
}
