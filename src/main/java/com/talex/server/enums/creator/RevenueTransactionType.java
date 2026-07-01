package com.talex.server.enums.creator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RevenueTransactionType {
    PREMIUM_SHARE,
    WITHDRAWAL,
    PENALTY_DEDUCTION,
    CAMPAIGN_REFUND,
    CONTENT_SHARE
}
