package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, String>, JpaSpecificationExecutor<PayoutRequest> {
}