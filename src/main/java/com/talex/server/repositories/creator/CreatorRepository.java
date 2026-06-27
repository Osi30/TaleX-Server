package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, String>, JpaSpecificationExecutor<Creator> {
    Optional<Creator> findByAccount_AccountId(UUID accountId);
}
