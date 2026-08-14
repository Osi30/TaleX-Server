package com.talex.server.mappers.series.impls;

import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.dtos.responses.series.CategoryResponseDto;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.dtos.responses.series.SeriesTrendingResponseDto;
import com.talex.server.dtos.responses.series.TagResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.series.Series;
import com.talex.server.mappers.series.CategoryMapper;
import com.talex.server.mappers.series.SeriesMapper;
import com.talex.server.mappers.series.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SeriesMapperImpl implements SeriesMapper {
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    @Override
    public SeriesCardResponseDto toCardDto(Series series) {
        Account account = series.getCreator().getAccount();

        return SeriesCardResponseDto.builder()
                .seriesId(series.getSeriesId())
                .accountId(account.getAccountId())
                .creatorId(series.getCreator().getCreatorId())
                .creatorName(account.getUsername())
                .creatorAvatar(account.getAvatarUrl())
                .totalCreatorFollowers(account.getTotalFollowersBy())
                .title(series.getTitle())
                .description(series.getDescription())
                .coverUrl(series.getCoverUrl())
                .bannerUrl(series.getBannerUrl())
                .contentType(series.getContentType())
                .ageRating(series.getAgeRating())
                .language(series.getLanguage())
                .totalViews(series.getAnalyticData().getViews())
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .averageRating(series.getAverageRating())
                .releasedUpdateTime(series.getReleasedUpdateTime())
                .build();
    }

    @Override
    public SeriesTrendingResponseDto toTrendingDto(Series series) {
        return SeriesTrendingResponseDto.builder()
                .seriesId(series.getSeriesId())
                .analyticData(series.getAnalyticData())
                .title(series.getTitle())
                .coverUrl(series.getCoverUrl())
                .bannerUrl(series.getBannerUrl())
                .trendingAnalyticData(series.getTrendingAnalyticData())
                .ratingCount(series.getRatingCount())
                .totalRating(series.getTotalRating())
                .averageRating(series.getAverageRating())
                .build();
    }

    @Override
    public SeriesResponseDto toResponse(Series series) {
        List<CategoryResponseDto> categories = series.getSeriesCategories()
                .stream().map(s -> categoryMapper.toResponse(s.getCategory()))
                .toList();

        List<TagResponseDto> tags = series.getSeriesTags()
                .stream().map(s -> tagMapper.toResponse(s.getTag()))
                .toList();

        Account account = series.getCreator().getAccount();

        return SeriesResponseDto.builder()
                .seriesId(series.getSeriesId())
                .accountId(series.getCreator().getAccount().getAccountId().toString())
                .creatorId(series.getCreator().getCreatorId())
                .creatorName(account.getFullName())
                .creatorAvatar(account.getAvatarUrl())
                .totalCreatorFollowers(series.getCreator().getAccount().getTotalFollowersBy())
                .title(series.getTitle())
                .description(series.getDescription())
                .coverUrl(series.getCoverUrl())
                .bannerUrl(series.getBannerUrl())
                .contentType(series.getContentType())
                .status(series.getStatus())
                .ageRating(series.getAgeRating())
                .contentWarnings(series.getContentWarnings())
                .language(series.getLanguage())
                .analyticData(series.getAnalyticData())
                .averageRating(series.getAverageRating())
                .categories(categories)
                .tags(tags)
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .deletedAt(series.getDeletedAt())
                .isDeleted(series.getIsDeleted())
                .build();
    }
}
