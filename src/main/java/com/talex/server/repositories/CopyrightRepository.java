package com.talex.server.repositories;

import com.talex.server.entities.media.Copyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CopyrightRepository extends JpaRepository<Copyright, String> {

    Optional<Copyright> findByCode(String code);

    List<Copyright> findByIsActiveTrue();
}
