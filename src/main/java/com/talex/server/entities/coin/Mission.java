package com.talex.server.entities.coin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cấu hình một nhiệm vụ trong Mission System.
 * <p>
 * - Admin tạo/sửa/vô hiệu hóa nhiệm vụ thông qua Admin API.
 * - {@code code} là định danh nghiệp vụ (business key) — duy nhất toàn hệ thống.
 * - {@code isActive} cho phép Admin bật/tắt nhiệm vụ mà không cần xoá dữ liệu.
 * - Kế thừa {@link BaseAudit} để theo dõi Admin nào tạo/sửa nhiệm vụ.
 * </p>
 */
@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mission_id", updatable = false, nullable = false)
    private UUID missionId;

    /**
     * Mã định danh nghiệp vụ của nhiệm vụ.
     * Ví dụ: "WATCH_AD_DAILY", "ONLINE_30_MIN", "COMPLETE_PROFILE".
     * Dùng để mapping từ event/hành động phía service → nhiệm vụ tương ứng.
     */
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    /**
     * Tên nhiệm vụ hiển thị thân thiện cho người dùng.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Mô tả cách thực hiện nhiệm vụ (hiển thị trên UI).
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Số coin thưởng khi hoàn thành nhiệm vụ.
     * DECIMAL(19,4) đảm bảo độ chính xác tài chính.
     */
    @Column(name = "reward_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    /**
     * Chỉ tiêu cần đạt để hoàn thành nhiệm vụ.
     * Ví dụ: xem 2 lần quảng cáo → targetValue = 2; online 30 phút → targetValue = 30.
     */
    @Column(name = "target_value", nullable = false)
    private int targetValue;

    /**
     * Trạng thái Bật/Tắt của nhiệm vụ.
     * false → nhiệm vụ bị ẩn khỏi danh sách của user và không tính tiến độ.
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    @JsonProperty("isActive")
    private boolean isActive = true;

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }
}
