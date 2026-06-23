package com.talex.server.entities.coin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bản ghi tiến độ thực hiện nhiệm vụ của một tài khoản trong một ngày.
 * <p>
 * - Không kế thừa {@code BaseAudit} để giữ bảng gọn nhẹ, tránh các cột audit không cần thiết.
 * - {@code progressDate} dùng để reset tiến độ qua ngày mới — mỗi ngày là một bản ghi độc lập.
 * - UNIQUE CONSTRAINT trên {@code (account_id, mission_id, progress_date)} là lớp bảo vệ
 *   cuối cùng chống race condition ở tầng database.
 * - {@code accountId} và {@code missionId} là Logical FK — KHÔNG dùng {@code @ManyToOne}.
 * </p>
 */
@Entity
@Table(
        name = "user_mission_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_mission_progress_account_mission_date",
                columnNames = {"account_id", "mission_id", "progress_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMissionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "progress_id", updatable = false, nullable = false)
    private UUID progressId;

    /**
     * Logical FK trỏ đến bảng accounts.
     * Không dùng @ManyToOne để giữ tính độc lập module.
     */
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    /**
     * Logical FK trỏ đến bảng missions.
     * Không dùng @ManyToOne để tránh N+1 không kiểm soát.
     */
    @Column(name = "mission_id", nullable = false, updatable = false)
    private UUID missionId;

    /**
     * Ngày thực hiện nhiệm vụ (chỉ phần date, không có time).
     * Cùng với account_id và mission_id tạo thành khoá tự nhiên (Natural Key) của bảng.
     * Tiến độ sẽ được reset qua ngày mới bằng cách tạo bản ghi mới với progressDate mới.
     */
    @Column(name = "progress_date", nullable = false, updatable = false)
    private LocalDate progressDate;

    /**
     * Tiến độ hiện tại của user đối với nhiệm vụ này trong ngày.
     * So sánh với {@code Mission.targetValue} để xác định đã hoàn thành chưa.
     */
    @Builder.Default
    @Column(name = "current_value", nullable = false)
    private int currentValue = 0;

    /**
     * Đánh dấu user đã hoàn thành nhiệm vụ (currentValue >= targetValue).
     * Khi true → không tăng tiến độ thêm và không thưởng coin lần nữa.
     */
    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    /**
     * Thời điểm hoàn thành nhiệm vụ. Null nếu chưa hoàn thành.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Thời điểm tạo bản ghi. Tự động điền bởi Hibernate, không cho phép sửa.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm cập nhật lần cuối. Tự động cập nhật bởi Hibernate.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
