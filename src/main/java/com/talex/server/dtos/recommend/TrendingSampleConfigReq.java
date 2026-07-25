package com.talex.server.dtos.recommend;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingSampleConfigReq {
    @NotNull(message = "minBatch không được để trống")
    @Min(value = 1, message = "minBatch phải lớn hơn 0")
    private Integer minBatch;

    @NotNull(message = "percentile không được để trống")
    @Min(value = 1, message = "percentile phải lớn hớn 0")
    @Max(value = 100, message = "percentile không được vượt quá 100")
    private Double percentile;

    @NotNull(message = "minImpression không được để trống")
    @Min(value = 50, message = "minImpression phải lớn hơn hoặc bằng 50")
    private Long minImpression;

    @NotNull(message = "maxImpression không được để trống")
    @Min(value = 100, message = "maxImpression phải lớn hơn hoặc bằng 100")
    private Long maxImpression;

    @NotNull(message = "gravity không được để trống")
    @Min(value = 0, message = "gravity phải lớn hơn 0")
    private Double gravity;
}
