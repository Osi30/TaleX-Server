package com.talex.server.repositories;

import com.talex.server.entities.Media;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, String> {
    Optional<Media> findByMediaIdAndIsDeletedFalse(String mediaId);

    Optional<Media> findFirstByChecksumAndIsDeletedFalse(String checksum);

    boolean existsByChecksumAndIsDeletedFalse(String checksum);

    List<Media> findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(String episodeId);

    List<Media> findAllByEpisode_EpisodeIdAndStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
            String episodeId,
            MediaStatus status);

    List<Media> findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses);

    Optional<Media> findFirstByEpisode_EpisodeIdAndMediaTypeAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            String episodeId,
            MediaType mediaType,
            MediaStatus status);
}
