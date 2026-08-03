package com.talex.server.services.subscription;

import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SubscriptionStatService {
    void upsertSubscriptionStat(UUID accountId, String creatorId, LocalDateTime startTime);

    /**
     * Tính toán hằng số Gamma và chia doanh thu theo Rule X (Fraud-Proof)
     */
    RuleXCalculationResponseDto calculateRuleX(RuleXCalculationRequestDto request);
}
