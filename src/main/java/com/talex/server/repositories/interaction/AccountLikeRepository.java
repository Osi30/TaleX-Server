package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AccountLikeRepository extends JpaRepository<AccountLike, String> {
    @Modifying
    @Query(value = "INSERT INTO account_likes (account_like_id, account_id, episode_id, created_at) " +
            "VALUES (gen_random_uuid(), :accountId, :episodeId, :timestamp) " +
            "ON CONFLICT (account_id, episode_id) DO NOTHING",
            nativeQuery = true)
    int insertLikeDirectly(
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Modifying
    @Query("DELETE FROM AccountLike al " +
            "WHERE al.account.accountId = :accountId " +
            "AND al.episode.episodeId = :episodeId")
    int deleteByAccountIdAndEpisodeId(
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId
    );

    @Query("SELECT al " +
            "FROM AccountLike al JOIN FETCH al.account " +
            "WHERE al.episode.episodeId = :episodeId")
    Slice<AccountLike> findByEpisodeEpisodeId(
            @Param("episodeId") String episodeId,
            Pageable pageable
    );

    @Query("SELECT al FROM AccountLike al " +
            "JOIN FETCH al.episode e " +
            "JOIN FETCH e.season s " +
            "JOIN FETCH s.series ser " +
            "WHERE al.account.accountId = :accountId")
    Slice<AccountLike> findByAccountAccountId(
            @Param("accountId") UUID accountId,
            Pageable pageable
    );
}