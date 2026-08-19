package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRevenueLogDetailResponseDto {
    private String id;
    private String subscriptionResultId;
    private String monthYear;
    private Double revenue;

    // Thông tin Creator & Account
    private String creatorId;
    private String username;
    private String avatarUrl;

    // Thông tin Episode
    private String episodeId;
    private String episodeTitle;
    private Integer episodeNumber;

    // Thông tin Series
    private String seriesId;
    private String seriesTitle;
    private String coverUrl;
    private String bannerUrl;
}