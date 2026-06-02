package com.talex.server.repositories;

import com.talex.server.entities.CreatorIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreatorIdentityRepository extends JpaRepository<CreatorIdentity, String> {
    Optional<CreatorIdentity> findByCreator_CreatorId(String creatorId);
}
