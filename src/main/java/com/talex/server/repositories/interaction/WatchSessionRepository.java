package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.WatchSession;
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
    @Query(value = "INSERT INTO watch_session " +
            "(watch_session_id, account_id, episode_id, watch_duration, heartbeat_count, start_time, end_time, current_position, updated_at) VALUES " +
            "(:watchSessionId, :accountId, :episodeId, 0.0, 0, :timestamp, :timestamp, 0.0, NOW()) " +
            "ON CONFLICT (watch_session_id) DO NOTHING", nativeQuery = true)
    void initializeDefaultSession(
            @Param("watchSessionId") String watchSessionId,
            @Param("accountId") UUID accountId,
            @Param("episodeId") String episodeId,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE watch_session " +
            "SET watch_duration = watch_duration + :heartbeatValue, " +
            "    heartbeat_count = heartbeat_count + 1, " +
            "    current_position = :currentPosition, " +
            "    end_time = :heartbeatTime, " +
            "    updated_at = NOW() " +
            "WHERE watch_session_id = :sessionId " +
            "  AND episode_id = :episodeId " +
            "  AND :heartbeatTime > end_time " +
            "  AND (EXTRACT(EPOCH FROM (:heartbeatTime - end_time)) >= :heartbeatValue - 1.0)",
            nativeQuery = true)
    int updateSession(
            @Param("sessionId") String sessionId,
            @Param("episodeId") String episodeId,
            @Param("currentPosition") Double currentPosition,
            @Param("heartbeatValue") Double heartbeatValue,
            @Param("heartbeatTime") LocalDateTime heartbeatTime
    );
}