package com.talex.server.dtos.recommend.request;

import jakarta.validation.constraints.*;
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
    @Max(value = 2000000, message = "minBatch lớn nhất là 2000000")
    private Integer minBatch;

    @NotNull(message = "percentile không được để trống")
    @Min(value = 1, message = "percentile phải lớn hớn 0")
    @Max(value = 100, message = "percentile không được vượt quá 100")
    private Double percentile;

    @NotNull(message = "minImpression không được để trống")
    @Min(value = 50, message = "minImpression phải lớn hơn hoặc bằng 50")
    @Max(value = 2000000, message = "minImpression lớn nhất là 2000000")
    private Long minImpression;

    @NotNull(message = "maxImpression không được để trống")
    @Min(value = 100, message = "maxImpression phải lớn hơn hoặc bằng 100")
    @Max(value = 2000000, message = "maxImpression lớn nhất là 2000000")
    private Long maxImpression;

    @NotNull(message = "gravity không được để trống")
    @DecimalMin(value = "0.01", message = "gravity phải lớn hơn 0.01")
    @DecimalMax(value = "20.0", message = "gravity phải nhỏ hơn 20.0")
    private Double gravity;

    @NotNull(message = "confidence score không được để trống")
    @DecimalMin(value = "0.01", message = "confidence score phải lớn hơn 0.01")
    @DecimalMax(value = "20.0", message = "confidence score phải nhỏ hơn 20.0")
    private Double confidenceScore;
}
