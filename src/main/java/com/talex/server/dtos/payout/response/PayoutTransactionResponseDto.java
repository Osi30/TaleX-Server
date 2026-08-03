package com.talex.server.dtos.payout.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutTransactionResponseDto {
    private String id;
    private String referenceId;
    private Long amount;
    private String description;
    private String toBin;
    private String toAccountNumber;
    private String toAccountName;
    private String state;
}