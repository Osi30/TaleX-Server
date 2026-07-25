package com.talex.server.entities.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trending_sample_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingSampleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id")
    private String configId;

    // M_total_batch: Tổng số series đã hoàn tất Vòng 1
    @Column(name = "total_batch", nullable = false)
    @Builder.Default
    private Long totalBatch = 0L;

    // M_total_batch: Tổng số series đã hoàn tất Vòng 1
    @Column(name = "calculated_batch", nullable = false)
    @Builder.Default
    private Long calculatedBatch = 0L;

    // M_min_batch: Số mẫu tối thiểu để bắt đầu tính ngưỡng lịch sử (Mặc định: 50)
    @Column(name = "min_batch", nullable = false)
    @Builder.Default
    private Integer minBatch = 50;

    // T_history: Ngưỡng điểm chuẩn tính từ Bách phân vị (VD: P50/P70) của lịch sử
    @Column(name = "threshold", nullable = false, columnDefinition = "float8 default 0.0")
    @Builder.Default
    private Double threshold = 0.0;

    // M_batch: Số series hoàn tất Vòng 1 trong đợt hiện tại (reset khi recalculate threshold)
    @Column(name = "current_batch", nullable = false)
    @Builder.Default
    private Integer currentBatch = 0;

    // Bách phân vị áp dụng để tính điểm chuẩn (VD: 70.0 tức P70)
    @Column(name = "percentile", nullable = false, columnDefinition = "float8 default 70.0")
    @Builder.Default
    private Double percentile = 70.0;

    // M_min_impression: Số hiển thị tối thiểu để đủ điều kiện chấm điểm Wilson (VD: 100)
    @Column(name = "min_impression", nullable = false)
    @Builder.Default
    private Long minImpression = 100L;

    // M_max_impression: Số hiển thị tối đa giới hạn cho Vòng 1 thử nghiệm (VD: 200)
    @Column(name = "max_impression", nullable = false)
    @Builder.Default
    private Long maxImpression = 200L;

    @Column(name = "gravity", nullable = false, columnDefinition = "float8 default 1.8")
    @Builder.Default
    private Double gravity = 1.8;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
