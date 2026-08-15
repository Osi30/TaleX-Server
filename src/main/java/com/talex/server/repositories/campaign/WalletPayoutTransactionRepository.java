package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.WalletPayoutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletPayoutTransactionRepository extends JpaRepository<WalletPayoutTransaction, String> {
    List<WalletPayoutTransaction> findByPayoutRequest_PayoutRequestId(String payoutRequestId);
}