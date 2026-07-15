package com.talex.server.repositories.auth;

import com.talex.server.entities.auth.Account;
import com.talex.server.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByAccountIdAndStatus(UUID accountId, AccountStatus status);

    Optional<Account> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Account> findByGoogleSubId(String googleSubId);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndAccountIdNot(String username, UUID accountId);
}
