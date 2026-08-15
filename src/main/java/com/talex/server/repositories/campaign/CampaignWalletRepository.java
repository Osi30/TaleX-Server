package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignWalletRepository extends JpaRepository<CampaignWallet, String> {

    // Tìm ví quảng cáo dựa trên accountId của Creator
    Optional<CampaignWallet> findByCreator_Account_AccountId(UUID accountId);

    // Tìm ví theo creatorId
    Optional<CampaignWallet> findByCreator_CreatorId(String creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CampaignWallet w WHERE w.creator.account.accountId = :accountId")
    Optional<CampaignWallet> findWithLockByAccountId(@Param("accountId") UUID accountId);
}