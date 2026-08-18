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

    // Admin hard-purge guard: block purge of a media that is the ORIGINAL SOURCE of another
    // media's copyright-violation record. Hard-deleting it would break the source_media_id link /
    // erase the evidence that the other media plagiarized this one — Admin must resolve those
    // violation(s) first.
    boolean existsBySourceMedia_MediaId(String sourceMediaId);

    // Admin hard-purge: remove all violation records where this media is the TARGET (single media,
    // not a batch — distinct from deleteAllByMedia_MediaIdIn used by cascade).
    void deleteAllByMedia_MediaId(String mediaId);
}
