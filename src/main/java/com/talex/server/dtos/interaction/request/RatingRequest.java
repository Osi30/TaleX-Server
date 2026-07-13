package com.talex.server.dtos.interaction.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RatingRequest {
    @NotNull(message = "Điểm đánh giá không được để trống")
    @DecimalMin(value = "0.1", message = "Đánh giá thấp nhất là 0.1 sao")
    @DecimalMax(value = "5.0", message = "Đánh giá cao nhất là 5 sao")
    private Double rate;

    @NotBlank(message = "Series đánh giá không được để trống")
    private String seriesId;

    @JsonIgnore
    private UUID accountId;
}