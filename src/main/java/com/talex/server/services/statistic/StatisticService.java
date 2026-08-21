package com.talex.server.services.statistic;

import com.talex.server.dtos.statistics.StatisticResponseDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueDetailDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueOverviewDto;
import com.talex.server.dtos.statistics.content.ContentRevenueDetailDto;
import com.talex.server.dtos.statistics.content.ContentRevenueOverviewDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueDetailDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueOverviewDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticService {
    StatisticResponseDto getOrderStatistics(LocalDateTime startTime, LocalDateTime endTime);

    // Campaign
    CampaignRevenueOverviewDto getCampaignOverview(LocalDateTime startTime, LocalDateTime endTime);
    List<CampaignRevenueDetailDto> getCampaignDetails(LocalDateTime startTime, LocalDateTime endTime);

    // Combo & Episode
    ContentRevenueOverviewDto getContentOverview(LocalDateTime startTime, LocalDateTime endTime);
    List<ContentRevenueDetailDto> getContentDetails(LocalDateTime startTime, LocalDateTime endTime);

    // Premium
    SubscriptionRevenueOverviewDto getSubscriptionOverview(LocalDateTime startTime, LocalDateTime endTime);
    List<SubscriptionRevenueDetailDto> getSubscriptionDetails(LocalDateTime startTime, LocalDateTime endTime);
}