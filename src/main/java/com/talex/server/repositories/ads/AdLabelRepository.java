package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdLabelRepository extends JpaRepository<AdLabel, UUID> {
    List<AdLabel> findAllByProfile_Account_AccountId(UUID accountId);
}
