package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.Creator;
import com.talex.server.records.CreatorVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, String>, JpaSpecificationExecutor<Creator> {
    Optional<Creator> findByAccount_AccountId(UUID accountId);

    @Query("SELECT c.creatorId FROM Creator c WHERE c.account.accountId = :accountId")
    Optional<String> findCreatorIdByAccountId(@Param("accountId") UUID accountId);

    @Query(value = "SELECT " +
            "  COALESCE(c.is_verified, false) AS isCreatorVerified, " +
            "  EXISTS ( " +
            "    SELECT 1 FROM terms_logs tl " +
            "    JOIN terms_versions tv ON tl.version_id = tv.id " +
            "    WHERE tl.account_id = c.account_id " +
            "      AND tv.type = 'CREATOR_ENABLE_MONETIZATION' " +
            "      AND tv.is_active = true " +
            "  ) AS isTermsAccepted, " +
            // Các trường lấy từ Creator Identity
            "  ci.status AS identityStatus, " +
            "  ci.verified_at AS identityVerifiedAt, " +
            "  ci.verified_note AS identityVerifiedNote, " +
            "  ci.tax_id AS taxId, " +
            // Các trường lấy từ Payment Profile (Chỉ lấy tài khoản primary)
            "  pp.status AS paymentStatus, " +
            "  pp.verified_at AS paymentVerifiedAt, " +
            "  pp.verified_note AS paymentVerifiedNote " +
            "FROM creator c " +
            "LEFT JOIN creator_identity ci ON c.creator_id = ci.creator_id " +
            "LEFT JOIN payment_profile pp ON c.creator_id = pp.creator_id AND pp.is_primary = true " +
            "WHERE c.account_id = :accountId", nativeQuery = true)
    Optional<CreatorVerificationStatus> getVerificationStatusByAccountId(@Param("accountId") String accountId);
}
