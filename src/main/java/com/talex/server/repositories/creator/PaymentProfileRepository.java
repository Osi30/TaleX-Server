package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.PaymentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProfileRepository
        extends JpaRepository<PaymentProfile, String>, JpaSpecificationExecutor<PaymentProfile> {

    Optional<PaymentProfile> findByPaymentProfileIdAndIsDeletedFalse(String id);

    Optional<PaymentProfile> findByCreator_Account_AccountIdAndIsPrimaryTrueAndIsDeletedFalse(UUID accountId);

    List<PaymentProfile> findByCreator_Account_AccountIdAndIsDeletedFalse(UUID accountId);

    @Modifying
    @Query("UPDATE PaymentProfile pp SET pp.isPrimary = false WHERE pp.creator.creatorId = :creatorId AND pp.paymentProfileId <> :id")
    void unsetOtherPrimary(@Param("creatorId") String creatorId, @Param("id") String id);

    @Query("SELECT COUNT(pp) FROM PaymentProfile pp WHERE pp.creator.creatorId = :creatorId")
    long countByCreatorId(@Param("creatorId") String creatorId);
}
