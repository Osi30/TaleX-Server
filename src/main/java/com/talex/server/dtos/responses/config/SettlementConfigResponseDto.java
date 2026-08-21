package com.talex.server.dtos.responses.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementConfigResponseDto {
    private String id;
    private BigDecimal minBalanceThreshold;
    private LocalDateTime updatedAt;
}