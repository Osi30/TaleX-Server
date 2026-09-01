package com.talex.server.dtos.responses.coin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInMilestoneDto {
    private Integer day;
    private BigDecimal rewardAmount;
}
