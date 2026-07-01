package com.talex.server.repositories;

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
}
