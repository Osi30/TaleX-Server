package com.talex.server.repositories.media;

import com.talex.server.entities.media.ContentCensorship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentCensorshipRepository extends JpaRepository<ContentCensorship, String> {

    List<ContentCensorship> findAllByMedia_MediaId(String mediaId);

    Optional<ContentCensorship> findFirstByMedia_MediaIdOrderByCheckedAtDesc(String mediaId);

    long countByMedia_CreatorIdAndStatus(String creatorId, com.talex.server.enums.media.CensorshipStatus status);

    // Bulk delete (1 DELETE statement) cho cascade xóa nhiều media cùng lúc (Series/Season/
    // Episode) — tránh N+1 query nếu gọi findAllByMedia_MediaId() lặp lại cho từng media.
    void deleteAllByMedia_MediaIdIn(Collection<String> mediaIds);
}
