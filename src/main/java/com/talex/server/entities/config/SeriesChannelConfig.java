package com.talex.server.entities.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "series_channel_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id")
    private String configId;

    @Column(name = "trending_pool_number")
    @Builder.Default
    private Integer trendingPoolNumber = 0;

    @Column(name = "promoted_pool_number")
    @Builder.Default
    private Integer promotedPoolNumber = 0;

    @Column(name = "new_released_pool_number")
    @Builder.Default
    private Integer newReleasedPoolNumber = 0;

    @Column(name = "latest_community_choice_pool_number")
    @Builder.Default
    private Integer latestCommunityChoicePoolNumber = 0;

    @Column(name = "community_choice_pool_number")
    @Builder.Default
    private Integer communityChoicePoolNumber = 0;

    @Column(name = "recently_updated_pool_number")
    @Builder.Default
    private Integer recentlyUpdatedPoolNumber = 0;

    @Column(name = "random_category_pool_number")
    @Builder.Default
    private Integer randomCategoryPoolNumber = 0;

    @Column(name = "subscribed_pool_number")
    @Builder.Default
    private Integer subscribedPoolNumber = 0;

    @Column(name = "number_per_category")
    @Builder.Default
    private Integer numberPerCategory = 0;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}