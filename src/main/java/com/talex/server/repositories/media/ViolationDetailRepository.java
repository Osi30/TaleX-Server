package com.talex.server.repositories.media;

import com.talex.server.entities.media.ViolationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationDetailRepository extends JpaRepository<ViolationDetail, String> {
}
