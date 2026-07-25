package com.talex.server.dtos.recommend;

import com.talex.server.entities.config.TrendingSampleConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingSampleConfigRes {
    private String configId;
    private Long totalBatch;
    private Integer minBatch;
    private Double threshold;
    private Integer currentBatch;
    private Double percentile;
    private Long minImpression;
    private Long maxImpression;
    private LocalDateTime updatedAt;
    private Double gravity;

    public static TrendingSampleConfigRes fromEntity(TrendingSampleConfig entity) {
        if (entity == null) return null;
        return TrendingSampleConfigRes.builder()
                .configId(entity.getConfigId())
                .totalBatch(entity.getTotalBatch())
                .minBatch(entity.getMinBatch())
                .threshold(entity.getThreshold())
                .currentBatch(entity.getCurrentBatch())
                .percentile(entity.getPercentile())
                .minImpression(entity.getMinImpression())
                .maxImpression(entity.getMaxImpression())
                .updatedAt(entity.getUpdatedAt())
                .gravity(entity.getGravity())
                .build();
    }
}