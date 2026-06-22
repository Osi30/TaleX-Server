package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.CoinTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    /**
     * Lấy toàn bộ lịch sử giao dịch của một ví, sắp xếp từ mới nhất.
     * Dùng nội bộ hoặc khi cần export đầy đủ (không phân trang).
     */
    List<CoinTransaction> findByWalletIdOrderByChangedAtDesc(UUID walletId);

    /**
     * Lấy lịch sử giao dịch có phân trang — dùng cho API lịch sử của client.
     * Spring Data JPA tự sinh query từ method name kết hợp {@link Pageable}.
     *
     * @param walletId ID ví cần truy vấn
     * @param pageable Thông tin phân trang (page 0-based, size, sort)
     * @return Page chứa danh sách giao dịch và metadata phân trang
     */
    Page<CoinTransaction> findByWalletIdOrderByChangedAtDesc(UUID walletId, Pageable pageable);
}
