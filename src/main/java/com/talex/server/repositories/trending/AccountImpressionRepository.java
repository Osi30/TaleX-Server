package com.talex.server.repositories.trending;

import com.talex.server.entities.interaction.AccountImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountImpressionRepository extends JpaRepository<AccountImpression, String> {

    /**
     * 1. Thêm nhanh nhiều Impression theo accountId và list seriesIds bằng Native Query.
     * Sử dụng unnest() để truyền List vào SQL 1 lần duy nhất, lọc bỏ những cặp (accountId, seriesId) đã tồn tại.
     */
    @Modifying
    @Query(value = """
        INSERT INTO account_impressions (id, account_id, series_id, is_interacted, is_watched)
        SELECT gen_random_uuid(), :accountId, s_id, false, false
        FROM unnest(string_to_array(:seriesIdsCsv, ',')) AS s_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM account_impressions ai
            WHERE ai.account_id = :accountId AND ai.series_id = s_id
        )
        """, nativeQuery = true)
    int insertBatchIfNotExists(@Param("accountId") UUID accountId, @Param("seriesIdsCsv") String seriesIdsCsv);

    /**
     * 2. Cập nhật nhanh isInteracted = true.
     * Chỉ thực thi nếu isInteracted hiện tại đang là false (tránh trigger chạy thừa).
     */
    @Modifying
    @Query("""
            UPDATE AccountImpression ai
            SET ai.isInteracted = true
            WHERE ai.account.accountId = :accountId
              AND ai.series.seriesId = :seriesId
              AND ai.isInteracted = false
            """)
    int updateIsInteractedTrue(
            @Param("accountId") UUID accountId,
            @Param("seriesId") String seriesId
    );

    /**
     * 3. Cập nhật nhanh isWatched = true.
     * Chỉ thực thi nếu isWatched hiện tại đang là false (tránh trigger chạy thừa).
     */
    @Modifying
    @Query("""
            UPDATE AccountImpression ai
            SET ai.isWatched = true
            WHERE ai.account.accountId = :accountId
              AND ai.series.seriesId = :seriesId
              AND ai.isWatched = false
            """)
    int updateIsWatchedTrue(
            @Param("accountId") UUID accountId,
            @Param("seriesId") String seriesId
    );
}