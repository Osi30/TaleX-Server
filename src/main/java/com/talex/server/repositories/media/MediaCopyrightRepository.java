package com.talex.server.repositories.media;

import com.talex.server.entities.media.MediaCopyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MediaCopyrightRepository extends JpaRepository<MediaCopyright, String> {

    List<MediaCopyright> findAllByMedia_MediaId(String mediaId);

    long countByMedia_CreatorIdAndIsValidFalse(String creatorId);

    // Bulk delete (1 DELETE statement) cho cascade xóa nhiều media cùng lúc (Series/Season/
    // Episode) — tránh N+1 query nếu gọi findAllByMedia_MediaId() lặp lại cho từng media.
    void deleteAllByMedia_MediaIdIn(Collection<String> mediaIds);
}
