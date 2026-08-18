package com.talex.server.repositories.media;

import com.talex.server.entities.media.MediaPlaybackSession;
import com.talex.server.enums.media.MediaPlaybackSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaPlaybackSessionRepository extends JpaRepository<MediaPlaybackSession, String> {
    List<MediaPlaybackSession> findAllByMedia_MediaIdAndStatusAndIsDeletedFalse(
            String mediaId,
            MediaPlaybackSessionStatus status);

    List<MediaPlaybackSession> findAllByStatusAndExpiresAtBeforeAndIsDeletedFalse(
            MediaPlaybackSessionStatus status,
            LocalDateTime expiresAt);

    // Admin hard-purge: remove ALL playback-session rows of a media (including already soft-deleted
    // and expired ones). These are ephemeral tokens with no retention obligation.
    void deleteAllByMedia_MediaId(String mediaId);
}
