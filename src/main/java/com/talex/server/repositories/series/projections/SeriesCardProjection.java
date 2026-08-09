package com.talex.server.repositories.series.projections;

import java.sql.Timestamp;

// Interface projection cho native query searchPublicSeries — constructor-projection JPQL
// (kiểu "new com.talex.server...SeriesCardResponseDto(...)") không dùng được với native SQL,
// nên Spring Data map từng cột SELECT alias sang getter tương ứng ở đây. Timestamp (không
// phải LocalDateTime) để khớp chắc chắn với kiểu JDBC trả về từ native query, convert sang
// LocalDateTime ở tầng service (SeriesServiceImpl.toCardDto).
public interface SeriesCardProjection {
    String getSeriesId();

    String getAccountId(); // UUID dạng text — service parse UUID.fromString

    String getCreatorId();

    String getCreatorName();

    String getCreatorAvatar();

    Long getTotalCreatorFollowers();

    String getTitle();

    String getDescription();

    String getCoverUrl();

    String getBannerUrl();

    String getContentType(); // tên enum dạng text — service parse ContentType.valueOf

    String getAgeRating();

    String getLanguage();

    Long getTotalViews();

    Timestamp getCreatedAt();

    Timestamp getUpdatedAt();

    Double getAverageRating();

    Timestamp getReleasedUpdateTime();
}
