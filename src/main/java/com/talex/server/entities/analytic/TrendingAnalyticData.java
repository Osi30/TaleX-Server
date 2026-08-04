package com.talex.server.entities.analytic;

import com.talex.server.enums.interaction.ImpressionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingAnalyticData {

    // N_i: Tổng lượt hiển thị thử nghiệm Vòng 1 của Series
    @Column(name = "total_impression", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long totalImpression = 0L;

    // k_i: Số lượt click VÀ ở lại đọc >= 5 giây (is_watched = true)
    @Column(name = "trending_engage_click", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long engageClick = 0L;

    // l_i: Số lượt click VÀ tương tác (is_interacted = true)
    @Column(name = "trending_interaction_click", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long interactionClick = 0L;

    // p^_i: Tỉ lệ tương tác quan sát được (sampleRatio = engageClick / totalImpression)
    @Column(name = "trending_sample_ratio", nullable = false, columnDefinition = "float8 default 0.0")
    @Builder.Default
    private Double sampleRatio = 0.0;

    // Score_Wilson,i: Điểm sàn an toàn từ công thức khoảng tin cậy Wilson
    @Column(name = "trending_wilson_score", nullable = false, columnDefinition = "float8 default 0.0")
    @Builder.Default
    private Double wilsonScore = 0.0;

    // Hacker New Ranking Score ,i: Điểm từ công thức của Hacker New Ranking
    @Column(name = "ranking_score", nullable = false, columnDefinition = "float8 default 0.0")
    @Builder.Default
    private Double rankingScore = 0.0;

    // Trạng thái thử nghiệm Vòng 1
    @Enumerated(EnumType.STRING)
    @Column(name = "trending_impression_status", length = 30)
    @Builder.Default
    private ImpressionStatus impressionStatus = ImpressionStatus.ON_GOING;
}