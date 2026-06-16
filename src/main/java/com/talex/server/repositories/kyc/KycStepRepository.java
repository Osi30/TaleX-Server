package com.talex.server.repositories.kyc;

import com.fasterxml.jackson.databind.JsonNode;
import com.talex.server.entities.kyc.KycStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface KycStepRepository extends JpaRepository<KycStep, String>, JpaSpecificationExecutor<KycStep> {
    List<KycStep> findByKycSession_KycSessionId(String sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE KycStep k SET k.isSuccess = :isSuccess, k.message = :message, k.rawResponse = :rawResponse WHERE k.kycStepId = :id")
    void updateKycResult(
            @Param("id") String id,
            @Param("isSuccess") boolean isSuccess,
            @Param("message") String message,
            @Param("rawResponse") JsonNode rawResponse
    );
}
