package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdvertiseProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvertiseProfileRepository extends JpaRepository<AdvertiseProfile, UUID> {
    Optional<AdvertiseProfile> findByAccount_AccountId(UUID accountId);
}
