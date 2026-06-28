package com.talex.server.entities;

import com.talex.server.entities.series.Episode;
import com.talex.server.enums.MediaPlaybackSessionStatus;
import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_playback_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaPlaybackSession extends BaseAudit {
    @Id
    @Column(name = "playback_session_id", length = 80)
    private String playbackSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "viewer_id")
    private String viewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MediaProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_type", nullable = false, length = 40)
    private MediaProtectionType protectionType;

    @Column(name = "playback_url", columnDefinition = "TEXT")
    private String playbackUrl;

    @Column(name = "token_id")
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "ip_address_hash")
    private String ipAddressHash;

    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaPlaybackSessionStatus status = MediaPlaybackSessionStatus.ACTIVE;
}
