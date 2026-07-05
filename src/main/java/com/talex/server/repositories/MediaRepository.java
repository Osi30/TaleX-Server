package com.talex.server.repositories;

import com.talex.server.entities.media.Media;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, String> {
    Optional<Media> findByMediaIdAndIsDeletedFalse(String mediaId);

    List<Media> findAllByMediaIdInAndIsDeletedFalse(Collection<String> mediaIds);

    Optional<Media> findFirstByChecksumAndIsDeletedFalse(String checksum);

    List<Media> findAllByChecksumInAndIsDeletedFalse(Collection<String> checksums);

    Optional<Media> findFirstByProviderPublicIdAndIsDeletedFalse(String providerPublicId);

    List<Media> findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
            MediaProvider provider,
            Collection<MediaStatus> statuses,
            LocalDateTime updatedAt,
            Pageable pageable);

    boolean existsByProviderAndStatusInAndProviderPublicIdIsNotNullAndIsDeletedFalse(
            MediaProvider provider,
            Collection<MediaStatus> statuses);

    boolean existsByChecksumAndIsDeletedFalse(String checksum);

    List<Media> findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(String episodeId);

    List<Media> findAllByEpisode_EpisodeIdAndStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
            String episodeId,
            MediaStatus status);

    List<Media> findAllByEpisode_EpisodeIdAndStatusInAndIsDeletedFalseOrderByDisplayOrderAsc(
            String episodeId,
            Collection<MediaStatus> statuses);

    List<Media> findAllByEpisode_EpisodeIdAndStatusInAndApprovalStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
            String episodeId,
            Collection<MediaStatus> statuses,
            ContentApprovalStatus approvalStatus);

    List<Media> findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses);

    List<Media> findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses,
            ContentApprovalStatus approvalStatus);

    boolean existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses);

    boolean existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses,
            ContentApprovalStatus approvalStatus);

    long countByEpisode_EpisodeIdAndIsDeletedFalse(String episodeId);

    boolean existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
            String episodeId,
            ContentApprovalStatus approvalStatus);

    long countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses);

    boolean existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseAndMediaIdNot(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses,
            String mediaId);

    boolean existsByEpisode_EpisodeIdAndDisplayOrderInAndIsDeletedFalse(
            String episodeId,
            Collection<Integer> displayOrders);

    boolean existsByEpisode_EpisodeIdAndDisplayOrderInAndMediaIdNotInAndIsDeletedFalse(
            String episodeId,
            Collection<Integer> displayOrders,
            Collection<String> mediaIds);

    @Query("select coalesce(max(m.displayOrder), 0) from Media m where m.episode.episodeId = :episodeId and m.isDeleted = false")
    Integer findMaxDisplayOrderByEpisodeId(@Param("episodeId") String episodeId);

    Optional<Media> findFirstByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseOrderByCreatedAtDesc(
            String episodeId,
            MediaType mediaType,
            Collection<MediaStatus> statuses);

    Optional<Media> findFirstByEpisode_EpisodeIdAndMediaTypeAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            String episodeId,
            MediaType mediaType,
            MediaStatus status);

    // Paginated query for staff moderation — lists media awaiting review
    Page<Media> findByApprovalStatusAndIsDeletedFalse(ContentApprovalStatus approvalStatus, Pageable pageable);
}
