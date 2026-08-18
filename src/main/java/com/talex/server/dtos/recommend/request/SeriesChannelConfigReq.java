package com.talex.server.dtos.recommend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesChannelConfigReq {

    @NotNull(message = "trendingPoolNumber không được để trống")
    @PositiveOrZero(message = "trendingPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer trendingPoolNumber;

    @NotNull(message = "promotedPoolNumber không được để trống")
    @PositiveOrZero(message = "promotedPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer promotedPoolNumber;

    @NotNull(message = "newReleasedPoolNumber không được để trống")
    @PositiveOrZero(message = "newReleasedPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer newReleasedPoolNumber;

    @NotNull(message = "latestCommunityChoicePoolNumber không được để trống")
    @PositiveOrZero(message = "latestCommunityChoicePoolNumber phải lớn hơn hoặc bằng 0")
    private Integer latestCommunityChoicePoolNumber;

    @NotNull(message = "communityChoicePoolNumber không được để trống")
    @PositiveOrZero(message = "communityChoicePoolNumber phải lớn hơn hoặc bằng 0")
    private Integer communityChoicePoolNumber;

    @NotNull(message = "recentlyUpdatedPoolNumber không được để trống")
    @PositiveOrZero(message = "recentlyUpdatedPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer recentlyUpdatedPoolNumber;

    @NotNull(message = "randomCategoryPoolNumber không được để trống")
    @PositiveOrZero(message = "randomCategoryPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer randomCategoryPoolNumber;

    @NotNull(message = "subscribedPoolNumber không được để trống")
    @PositiveOrZero(message = "subscribedPoolNumber phải lớn hơn hoặc bằng 0")
    private Integer subscribedPoolNumber;

    @NotNull(message = "numberPerCategory không được để trống")
    @PositiveOrZero(message = "numberPerCategory phải lớn hơn hoặc bằng 0")
    private Integer numberPerCategory;
}