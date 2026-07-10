package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountBookmarkRepository extends JpaRepository<AccountBookmark, String> {

    @Modifying
    @Query(value = "INSERT INTO account_bookmarks (bookmark_id, account_id, episode_id, created_at) " +
            "VALUES (gen_random_uuid(), :accountId, :episodeId, NOW()) " +
            "ON CONFLICT (account_id, episode_id) DO NOTHING",
            nativeQuery = true)
    int insertBookmarkDirectly(
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId
    );

    @Modifying
    @Query("DELETE FROM AccountBookmark ab " +
            "WHERE ab.account.accountId = :accountId " +
            "AND ab.episode.episodeId = :episodeId")
    int deleteByAccountIdAndEpisodeId(
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId
    );

    @Query("SELECT ab FROM AccountBookmark ab JOIN FETCH ab.account " +
            "WHERE ab.episode.episodeId = :episodeId")
    Slice<AccountBookmark> findByEpisodeEpisodeId(
            @Param("episodeId") String episodeId,
            Pageable pageable
    );

    @Query("SELECT ab FROM AccountBookmark ab " +
            "JOIN FETCH ab.episode e " +
            "JOIN FETCH e.season s " +
            "JOIN FETCH s.series ser " +
            "WHERE ab.account.accountId = :accountId")
    Slice<AccountBookmark> findByAccountAccountId(
            @Param("accountId") UUID accountId,
            Pageable pageable
    );
}