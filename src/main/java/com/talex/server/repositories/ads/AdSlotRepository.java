package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdSlotRepository extends JpaRepository<AdSlot, UUID> {
    Optional<AdSlot> findByCodeName(String codeName);
    List<AdSlot> findByIsActiveTrue();
}
