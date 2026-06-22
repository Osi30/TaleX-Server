package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.CoinEconomyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoinEconomyConfigRepository extends JpaRepository<CoinEconomyConfig, UUID> {

    /**
     * Lấy ra cấu hình kinh tế mới nhất của hệ thống.
     * Hệ thống chỉ dùng bản ghi này để tính toán phần thưởng.
     */
    Optional<CoinEconomyConfig> findFirstByOrderByCreatedAtDesc();
}
