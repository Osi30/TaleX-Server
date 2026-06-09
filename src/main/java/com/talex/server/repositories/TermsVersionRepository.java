package com.talex.server.repositories;

import com.talex.server.entities.TermsVersion;
import com.talex.server.enums.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermsVersionRepository
        extends JpaRepository<TermsVersion, String>, JpaSpecificationExecutor<TermsVersion> {
    Optional<TermsVersion> findByTypeAndIsActiveTrue(TermsType type);

    /**
     * Cập nhật hàng loạt trạng Term Version
     */
    @Modifying
    @Query("UPDATE TermsVersion tv SET tv.isActive = :targetActive WHERE tv.isActive = :currentActive AND tv.type = :type")
    void bulkUpdateStatus(
            @Param("type") TermsType type,
            @Param("currentActive") boolean currentActive,
            @Param("targetActive") boolean targetActive
    );
}
