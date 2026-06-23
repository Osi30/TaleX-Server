package com.talex.server.repositories;

import com.talex.server.entities.WatchSession;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WatchSessionRepository extends JpaRepository<WatchSession, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO watch_session (watch_session_id, account_id, episode_id, creator_id, watch_duration, total_duration, heartbeat_count, start_time, end_time,  updated_at) " +
            "VALUES (:sessionId, :accountId, :episodeId, :creatorId, :watchDuration, :totalDuration, :heartbeatCount, :startTime, :endTime, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (watch_session_id) " +
            "DO UPDATE SET " +
            "    watch_duration = EXCLUDED.watch_duration, " +
            "    heartbeat_count = EXCLUDED.heartbeat_count, " +
            "    end_time = EXCLUDED.end_time, " +
            "    updated_at = CURRENT_TIMESTAMP", nativeQuery = true)
    void upsertWatchSession(
            @Param("sessionId") String sessionId,
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId,
            @Param("creatorId") String creatorId,
            @Param("watchDuration") Double watchDuration,
            @Param("totalDuration") Double totalDuration,
            @Param("heartbeatCount") Integer heartbeatCount,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}