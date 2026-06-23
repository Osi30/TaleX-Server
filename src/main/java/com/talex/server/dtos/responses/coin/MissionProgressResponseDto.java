package com.talex.server.dtos.responses.coin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionProgressResponseDto {

    private UUID missionId;
    private String code;
    private String title;
    private String description;
    private BigDecimal rewardAmount;
    private int targetValue;
    private int currentValue;
    private boolean isCompleted;

    @JsonProperty("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }
}
