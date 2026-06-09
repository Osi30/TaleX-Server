package com.talex.server.repositories;

import com.talex.server.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByEmail(String email);

    Optional<Account> findByUsername(String username);

    Optional<Account> findByGoogleSubId(String googleSubId);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndAccountIdNot(String username, UUID accountId);
}
