package com.talex.server.dtos.requests.coin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequestDto {

    @NotBlank(message = "Mã nhiệm vụ không được để trống")
    private String code;

    @NotBlank(message = "Tên nhiệm vụ không được để trống")
    private String title;

    @NotBlank(message = "Mô tả nhiệm vụ không được để trống")
    private String description;

    @NotNull(message = "Phần thưởng nhiệm vụ không được để trống")
    @Positive(message = "Phần thưởng nhiệm vụ phải lớn hơn 0")
    private BigDecimal rewardAmount;

    @Positive(message = "Mục tiêu nhiệm vụ phải lớn hơn 0")
    private int targetValue;

    private boolean isActive;

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }

    @JsonProperty("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }
}
