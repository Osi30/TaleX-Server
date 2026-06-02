package com.talex.server.repositories;

import com.talex.server.entities.CreatorTermsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreatorTermsLogRepository extends JpaRepository<CreatorTermsLog, String> {
    List<CreatorTermsLog> findByCreator_CreatorId(String creatorId);
}
