package com.talex.server.entities.coin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bản ghi điểm danh hằng ngày của một tài khoản.
 * <p>
 * - UNIQUE CONSTRAINT trên (account_id, check_in_date) là lớp bảo vệ cuối cùng
 *   chống Race Condition ở tầng database, hỗ trợ lớp Redis Distributed Lock ở tầng Service.
 * - Không kế thừa BaseAudit vì đây là bản ghi audit tự thân, chỉ cần createdAt.
 * - accountId là Logical FK — KHÔNG dùng @ManyToOne/@JoinColumn.
 * </p>
 */
@Entity
@Table(
        name = "daily_check_ins",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_check_ins_account_date",
                columnNames = {"account_id", "check_in_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checkin_id", updatable = false, nullable = false)
    private UUID checkinId;

    /**
     * Logical FK trỏ đến bảng accounts.
     * Không dùng @ManyToOne để giữ tính độc lập module.
     */
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    /**
     * Ngày điểm danh (chỉ phần date, không có time).
     * Cùng với account_id tạo thành khoá tự nhiên (Natural Key) của bảng.
     */
    @Column(name = "check_in_date", nullable = false, updatable = false)
    private LocalDate checkInDate;

    /**
     * Số ngày điểm danh liên tiếp tại thời điểm điểm danh này.
     * Dùng để tính streak multiplier cho phần thưởng.
     */
    @Column(name = "consecutive_days", nullable = false)
    private Integer consecutiveDays;

    /**
     * Số coin được thưởng cho lần điểm danh này (đã tính streak multiplier).
     * DECIMAL(19,4) để đảm bảo độ chính xác.
     */
    @Column(name = "reward_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    /**
     * Thời điểm điểm danh. Tự động điền bởi Hibernate, không cho phép sửa.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
