package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdTransactionRepository extends JpaRepository<AdTransaction, UUID> {
    List<AdTransaction> findByProfile_ProfileIdOrderByCreatedAtDesc(UUID profileId);
}
