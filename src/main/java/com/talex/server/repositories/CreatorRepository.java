package com.talex.server.repositories;

import com.talex.server.entities.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, String> {
//    Creator findByAccount_Id(String accountId);
}
