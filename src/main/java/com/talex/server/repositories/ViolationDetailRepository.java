package com.talex.server.repositories;

import com.talex.server.entities.ViolationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationDetailRepository extends JpaRepository<ViolationDetail, String> {
}
