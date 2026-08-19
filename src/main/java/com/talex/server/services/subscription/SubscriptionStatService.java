package com.talex.server.services.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.revenue.response.RuleXCalculationResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionStatDetailResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionStatResponseDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.subscription.SubscriptionResult;

import java.time.LocalDateTime;
import java.util.List;
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
    List<RuleXCalculationRequestDto> getRuleXRequestFromStats(String monthYear);

    // For Demo - Get Response
    List<SubscriptionResult> calculateAndSaveRevenue(String monthYear, boolean isDemo);

    BasePageResponse<SubscriptionStatDetailResponseDto> getDetailedStatsByAccountSubscriptionId(
            String accountSubscriptionId, int page, int pageSize);
}
