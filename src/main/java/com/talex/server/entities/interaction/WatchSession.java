package com.talex.server.entities.interaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "watch_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "watch_session_id", length = 50)
    private String id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "episode_id", length = 50, nullable = false)
    private String episodeId;

    @Column(name = "creator_id", length = 50, nullable = false)
    private String creatorId;

    @Column(name = "total_duration", nullable = false)
    private Double totalDuration;

    @Column(name = "watch_duration", nullable = false)
    private Double watchDuration;

    @Column(name = "heartbeat_count", nullable = false)
    private Integer heartbeatCount;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}