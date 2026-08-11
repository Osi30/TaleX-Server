package com.talex.server.services.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionStatResponseDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.subscription.SubscriptionResult;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SubscriptionStatService {
    BasePageResponse<SubscriptionStatResponseDto> getStatsByAccountSubscriptionId(
            String accountSubscriptionId, int page, int pageSize);

    int processSubscriptionStats();

    void upsertSubscriptionStat(UUID accountId, String creatorId, String episodeId, LocalDateTime startTime);

    /**
     * Tính toán hằng số Gamma và chia doanh thu theo Rule X (Fraud-Proof)
     */
    RuleXCalculationResponseDto calculateRuleX(RuleXCalculationRequestDto request);

    // For Demo - Get Request
    RuleXCalculationRequestDto getRuleXRequestFromStats(String monthYear, Subscription subscription);

    // For Demo - Get Response
    SubscriptionResult calculateAndSaveRevenue(String monthYear, Subscription subscription, boolean isDemo);
}
