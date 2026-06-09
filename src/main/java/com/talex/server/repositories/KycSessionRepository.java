package com.talex.server.repositories;

import com.talex.server.entities.KycSession;
import com.talex.server.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KycSessionRepository extends JpaRepository<KycSession, String>, JpaSpecificationExecutor<KycSession> {

    /**
     * Cập nhật hàng loạt trạng thái Session
     */
    @Modifying
    @Query("UPDATE KycSession k SET k.status = :newStatus WHERE k.status = :oldStatus AND k.creator.creatorId = :creatorId")
    void bulkUpdateStatus(
            @Param("creatorId") String creatorId,
            @Param("oldStatus") KycStatus oldStatus,
            @Param("newStatus") KycStatus newStatus
    );
}
