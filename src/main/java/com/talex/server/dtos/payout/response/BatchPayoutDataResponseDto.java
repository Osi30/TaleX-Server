package com.talex.server.dtos.payout.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPayoutDataResponseDto {
    private String id;
    private String referenceId;
    private List<PayoutTransactionResponseDto> transactions;
    private List<String> category;
    private String approvalState;
    private String createdAt;
}