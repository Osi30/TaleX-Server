package com.talex.server.repositories;

import com.talex.server.entities.media.MediaUploadSession;
import com.talex.server.enums.media.MediaUploadSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaUploadSessionRepository extends JpaRepository<MediaUploadSession, String> {
    Optional<MediaUploadSession> findByUploadSessionIdAndIsDeletedFalse(String uploadSessionId);

    List<MediaUploadSession> findAllByMedia_MediaIdAndIsDeletedFalse(String mediaId);

    List<MediaUploadSession> findAllByMedia_MediaIdInAndIsDeletedFalse(Collection<String> mediaIds);

    List<MediaUploadSession> findAllByStatusInAndExpiredAtBeforeAndIsDeletedFalse(
            Collection<MediaUploadSessionStatus> statuses,
            LocalDateTime expiredAt);
}
