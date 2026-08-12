package com.talex.server.dtos.recommend.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeFeedRequestDto {

    @Min(value = 1, message = "Promoted limit phải lớn hơn 0")
    @Max(value = 49, message = "Promoted limit phải nhỏ hơn 50")
    @Builder.Default
    private int promotedLimit = 5;

    @Min(value = 1, message = "Trending limit phải lớn hơn 0")
    @Max(value = 49, message = "Trending limit phải nhỏ hơn 50")
    @Builder.Default
    private int trendingLimit = 5;

    @Min(value = 1, message = "New Releases limit phải lớn hơn 0")
    @Max(value = 49, message = "New Releases limit phải nhỏ hơn 50")
    @Builder.Default
    private int newReleasesLimit = 5;

    @Min(value = 1, message = "Recently Updated limit phải lớn hơn 0")
    @Max(value = 49, message = "Recently Updated limit phải nhỏ hơn 50")
    @Builder.Default
    private int recentlyUpdatedLimit = 5;

    @Min(value = 1, message = "Latest Community Choice limit phải lớn hơn 0")
    @Max(value = 49, message = "Latest Community Choice limit phải nhỏ hơn 50")
    @Builder.Default
    private int latestCommunityChoiceLimit = 5;

    @Min(value = 1, message = "Community Choice limit phải lớn hơn 0")
    @Max(value = 49, message = "Community Choice limit phải nhỏ hơn 50")
    @Builder.Default
    private int communityChoiceLimit = 5;

    @Min(value = 1, message = "Random Category limit phải lớn hơn 0")
    @Max(value = 49, message = "Random Category limit phải nhỏ hơn 50")
    @Builder.Default
    private int randomCategoryLimit = 5;

    @Min(value = 1, message = "Subscription limit phải lớn hơn 0")
    @Max(value = 49, message = "Subscription limit phải nhỏ hơn 50")
    @Builder.Default
    private int subscriptionLimit = 5;
}