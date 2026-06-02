package com.talex.server.repositories;

import com.talex.server.entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    @Query("SELECT o FROM OtpVerification o WHERE o.account.accountId = :accountId "
            + "AND o.used = false AND o.expiresAt > CURRENT_TIMESTAMP "
            + "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestActiveOtp(@Param("accountId") UUID accountId);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.used = true "
            + "WHERE o.account.accountId = :accountId AND o.used = false")
    void invalidatePreviousOtps(@Param("accountId") UUID accountId);
}
