package com.talex.server.repositories.auth;

import com.talex.server.entities.auth.Account;
import com.talex.server.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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


    /// Tìm các ID tài khoản hết hạn cửa sổ 24h
    @Query("SELECT a.accountId " +
            "FROM Account a " +
            "WHERE a.is24h = false " +
            "AND a.lastInteractionTime <= :threshold")
    List<UUID> findExpired24hAccountIds(@Param("threshold") LocalDateTime threshold);

    /// Tìm các ID tài khoản hết hạn cửa sổ 7 ngày
    @Query("SELECT a.accountId " +
            "FROM Account a " +
            "WHERE a.is7d = false " +
            "AND a.lastInteractionTime <= :threshold")
    List<UUID> findExpired7dAccountIds(@Param("threshold") LocalDateTime threshold);

    /// Update hàng loạt trạng thái cờ is_24h
    @Modifying
    @Transactional
    @Query("UPDATE Account a " +
            "SET a.is24h = true " +
            "WHERE a.accountId IN :accountIds")
    void updateIs24hByAccountIds(
            @Param("accountIds") List<UUID> accountIds
    );

    /// Update hàng loạt trạng thái cờ is_7d
    @Modifying
    @Transactional
    @Query("UPDATE Account a " +
            "SET a.is7d = true " +
            "WHERE a.accountId IN :accountIds")
    void updateIs7dByAccountIds(
            @Param("accountIds") List<UUID> accountIds
    );
}
