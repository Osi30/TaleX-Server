package com.talex.server.enums.creator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WalletChangeType {
    REVENUE_SHARE,
    WITHDRAWAL_REQUEST,
    PENALTY_DEDUCTION,
    CAMPAIGN_REFUND,
    SINGLE_EPISODE_SHARE,
    SERIES_SEASONS_SHARE;
}
