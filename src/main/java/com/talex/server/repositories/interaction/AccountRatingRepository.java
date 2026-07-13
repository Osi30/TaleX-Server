package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountRating;
import com.talex.server.dtos.interaction.response.AccountRatingResponse;
import com.talex.server.dtos.interaction.response.SeriesRatingResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRatingRepository extends JpaRepository<AccountRating, String> {

    Optional<AccountRating> findByAccountAccountIdAndSeriesSeriesId(UUID accountId, String seriesId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE series " +
            "SET total_rating = total_rating + :ratingDelta, " +
            "    rating_count = rating_count + :countDelta, " +
            "    average_rating = CASE " +
            "        WHEN (rating_count + :countDelta) <= 0 THEN 0.0 " +
            "        ELSE CAST(total_rating + :ratingDelta AS double precision) / (rating_count + :countDelta) " +
            "    END " +
            "WHERE series_id = :seriesId ", nativeQuery = true)
    void updateSeriesRatingMetrics(
            @Param("seriesId") String seriesId,
            @Param("ratingDelta") double ratingDelta,
            @Param("countDelta") long countDelta
    );

    // Lấy tất cả đánh giá của một Account cụ thể
    @Query("SELECT new com.talex.server.dtos.interaction.response.AccountRatingResponse(" +
            "ar.series.seriesId, ar.series.title, ar.series.coverUrl, ar.series.bannerUrl, ar.rate, ar.createdAt, ar.updatedAt) " +
            "FROM AccountRating ar " +
            "WHERE ar.account.accountId = :accountId " +
            "ORDER BY ar.createdAt DESC")
    Slice<AccountRatingResponse> findRatingsByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    // Lấy tất cả đánh giá của một Series cụ thể
    @Query("SELECT new com.talex.server.dtos.interaction.response.SeriesRatingResponse(" +
            "ar.account.accountId, ar.account.username, ar.account.avatarUrl, ar.rate, ar.createdAt, ar.updatedAt) " +
            "FROM AccountRating ar " +
            "WHERE ar.series.seriesId = :seriesId " +
            "ORDER BY ar.createdAt DESC")
    Slice<SeriesRatingResponse> findRatingsBySeriesId(@Param("seriesId") String seriesId, Pageable pageable);

    boolean existsByAccountAccountIdAndSeriesSeriesId(UUID accountId, String seriesId);
}