package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.CoinWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoinWalletRepository extends JpaRepository<CoinWallet, UUID> {

    /**
     * Tìm ví theo accountId (Logical FK).
     * Trả về Optional.empty() nếu user chưa có ví → Service sẽ khởi tạo Lazy.
     */
    Optional<CoinWallet> findByAccountId(UUID accountId);
}
