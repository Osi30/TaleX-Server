package com.talex.server.dtos.payout.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutAccountBalanceResponseDto {
    private String accountNumber;
    private String accountName;
    private String currency;
    private String balance;
}