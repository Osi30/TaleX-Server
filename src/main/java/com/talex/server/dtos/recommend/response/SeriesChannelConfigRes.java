package com.talex.server.dtos.recommend.response;

import com.talex.server.entities.config.SeriesChannelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesChannelConfigRes {

    private String configId;
    private Integer trendingPoolNumber;
    private Integer promotedPoolNumber;
    private Integer newReleasedPoolNumber;
    private Integer latestCommunityChoicePoolNumber;
    private Integer communityChoicePoolNumber;
    private Integer recentlyUpdatedPoolNumber;
    private Integer randomCategoryPoolNumber;
    private Integer subscribedPoolNumber;
    private Integer numberPerCategory;
    private LocalDateTime updatedAt;

    public static SeriesChannelConfigRes fromEntity(SeriesChannelConfig entity) {
        if (entity == null) return null;
        return SeriesChannelConfigRes.builder()
                .configId(entity.getConfigId())
                .trendingPoolNumber(entity.getTrendingPoolNumber())
                .promotedPoolNumber(entity.getPromotedPoolNumber())
                .newReleasedPoolNumber(entity.getNewReleasedPoolNumber())
                .latestCommunityChoicePoolNumber(entity.getLatestCommunityChoicePoolNumber())
                .communityChoicePoolNumber(entity.getCommunityChoicePoolNumber())
                .recentlyUpdatedPoolNumber(entity.getRecentlyUpdatedPoolNumber())
                .randomCategoryPoolNumber(entity.getRandomCategoryPoolNumber())
                .subscribedPoolNumber(entity.getSubscribedPoolNumber())
                .numberPerCategory(entity.getNumberPerCategory())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}