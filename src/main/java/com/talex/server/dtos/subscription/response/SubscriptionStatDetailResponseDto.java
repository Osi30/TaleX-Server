package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatDetailResponseDto {
    private String id;
    private String monthYear;
    private String accountSubscriptionId;

    // Thông tin Creator
    private String creatorId;
    private String creatorUsername;
    private String creatorAvatarUrl;

    // Thông tin Episode
    private String episodeId;
    private String episodeTitle;
    private Integer episodeNumber;

    // Thông tin Series
    private String seriesId;
    private String seriesTitle;
    private String coverUrl;
    private String bannerUrl;

    // Lượt xem
    private Long views;
}