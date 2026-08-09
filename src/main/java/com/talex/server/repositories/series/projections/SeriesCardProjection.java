package com.talex.server.repositories.series.projections;

import java.time.LocalDateTime;

// Interface projection cho native query searchPublicSeries — constructor-projection JPQL
// (kiểu "new com.talex.server...SeriesCardResponseDto(...)") không dùng được với native SQL,
// nên Spring Data map từng cột SELECT alias sang getter tương ứng ở đây. LocalDateTime (không
// phải java.sql.Timestamp) vì Hibernate ở dự án này trả cột timestamp thẳng dưới dạng
// LocalDateTime cho cả native query lẫn JPQL — khai Timestamp ở đây khiến Spring Data báo lỗi
// "Cannot project java.time.LocalDateTime to java.sql.Timestamp" lúc runtime (đã verify qua
// log lỗi thật, không phải suy đoán).
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

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    Double getAverageRating();

    LocalDateTime getReleasedUpdateTime();
}
