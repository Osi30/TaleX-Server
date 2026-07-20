package com.talex.server.repositories.transaction;

import com.talex.server.entities.transaction.Invoice;
import com.talex.server.enums.transaction.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findTop50ByStatusOrderByCreatedAtAsc(InvoiceStatus status);

    List<Invoice> findByTransaction_TransactionIdIn(List<String> transactionIds);
}
