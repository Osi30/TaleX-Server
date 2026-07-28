package com.talex.server.dtos.responses.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdTransactionResponseDto {
    private UUID transactionId;
    private Long amount;
    private String type;
    private String note;
    private LocalDateTime createdAt;
}
