package com.talex.server.entities.interaction;

import com.talex.server.entities.Account;
import com.talex.server.entities.series.Episode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchSession {
    @Id
    @Column(name = "watch_session_id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "watch_duration", nullable = false)
    @Builder.Default
    private Double watchDuration = 0D;

    @Column(name = "heartbeat_count", nullable = false)
    @Builder.Default
    private Integer heartbeatCount = 0;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Builder.Default
    @Column(name = "current_position", nullable = false)
    private Double currentPosition = 0D;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}