package com.talex.server.repositories.media;

import com.talex.server.entities.media.MediaPurgeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaPurgeLogRepository extends JpaRepository<MediaPurgeLog, String> {
}
