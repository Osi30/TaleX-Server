package com.talex.server.repositories.coin;

import com.talex.server.entities.coin.DailyCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, UUID> {

    /**
     * Kiểm tra user đã điểm danh vào ngày cụ thể chưa.
     * Dùng để enforce idempotency ở tầng Application trước khi DB constraint bắt.
     */
    boolean existsByAccountIdAndCheckInDate(UUID accountId, LocalDate checkInDate);

    /**
     * Lấy bản ghi điểm danh gần nhất của user để tính streak (consecutive_days).
     */
    Optional<DailyCheckIn> findTopByAccountIdOrderByCheckInDateDesc(UUID accountId);

    /**
     * Lấy bản ghi điểm danh theo ngày cụ thể (phục vụ query lịch sử).
     */
    Optional<DailyCheckIn> findByAccountIdAndCheckInDate(UUID accountId, LocalDate checkInDate);
}
