package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorIdentityRepository extends JpaRepository<CreatorIdentity, String>, JpaSpecificationExecutor<CreatorIdentity> {
    Optional<CreatorIdentity> findByCreator_CreatorId(String creatorId);
    Optional<CreatorIdentity> findByCreator_Account_AccountId(UUID accountId);
    boolean existsByIdNumberAndKycSessionIsNotNull(String idNumber);
}
