package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignWalletTransaction;
import com.talex.server.enums.engagement.WalletReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignWalletTransactionRepository extends JpaRepository<CampaignWalletTransaction, String> {
    List<CampaignWalletTransaction> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            WalletReferenceType referenceType, String referenceId);

    Page<CampaignWalletTransaction> findByCampaignWallet_WalletIdOrderByCreatedAtDesc(
            String walletId, Pageable pageable);
}