package com.talex.server.repositories;

import com.talex.server.entities.TermsVersion;
import com.talex.server.enums.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermsVersionRepository
        extends JpaRepository<TermsVersion, String>, JpaSpecificationExecutor<TermsVersion> {
    Optional<TermsVersion> findByTypeAndIsActiveTrue(TermsType type);
}
