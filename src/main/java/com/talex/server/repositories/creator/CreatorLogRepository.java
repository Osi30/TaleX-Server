package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface CreatorLogRepository extends JpaRepository<CreatorLog, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, follows, likes, views, comments, shares, bookmarks, watch_time) " +
            "VALUES (gen_random_uuid(), :hourBucket, :followedId, :delta, 0, 0, 0, 0, 0, 0.0) " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET follows = COALESCE(creator_log.follows, 0) + :delta", nativeQuery = true)
    void upsertCreatorLogFollows(
            @Param("followedId") UUID followedId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );
}