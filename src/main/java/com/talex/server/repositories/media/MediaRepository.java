package com.talex.server.repositories.media;

import com.talex.server.entities.media.Media;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Includes soft-deleted rows — used to distinguish "deleted here mid-flight" from
    // "never existed in this environment" before re-firing fingerprint cleanup.
    Optional<Media> findByMediaId(String mediaId);

    Optional<Media> findByMediaIdAndCreatorIdAndIsDeletedFalse(String mediaId, String creatorId);

    // UPDATE nguyên tử thay vì read-then-write — nếu 1 kết quả pipeline hợp lệ đã commit
    // status/approvalStatus ra khỏi predicate ĐÚNG lúc reconciler quét, điều kiện WHERE
    // không khớp nữa, UPDATE ảnh hưởng 0 dòng, tránh ghi đè kết quả thật thành FAILED.
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Media m SET m.status = com.talex.server.enums.media.MediaStatus.FAILED,
               m.errorMessage = :errorMessage, m.updatedBy = :actor
        WHERE m.mediaId = :mediaId
          AND m.status IN :inFlightStatuses
          AND m.approvalStatus = com.talex.server.enums.series.ContentApprovalStatus.PENDING_REVIEW
          AND m.updatedAt < :staleBefore
          AND m.isDeleted = false
        """)
    int markStalePipelineFailed(@Param("mediaId") String mediaId,
                                 @Param("inFlightStatuses") Collection<MediaStatus> inFlightStatuses,
                                 @Param("staleBefore") LocalDateTime staleBefore,
                                 @Param("errorMessage") String errorMessage,
                                 @Param("actor") String actor);

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
    Page<Media> findByApprovalStatusAndStatusAndIsDeletedFalse(
            ContentApprovalStatus approvalStatus, MediaStatus status, Pageable pageable);

    // "Đã duyệt" tab — cho Staff/Admin xem lại nội dung đã duyệt để phát hiện lỡ bấm nhầm,
    // có thể ép ẩn (forceHide) nếu cần. Gồm cả FORCE_HIDDEN để admin thấy những gì mình đã
    // ép ẩn trước đó (và có thể bỏ ép ẩn lại), không chỉ ACTIVE/HLS_READY đang hiển thị công khai.
    Page<Media> findByApprovalStatusAndStatusInAndIsDeletedFalse(
            ContentApprovalStatus approvalStatus, Collection<MediaStatus> statuses, Pageable pageable);

    // Filter con của tab "Đã duyệt" — phân biệt nội dung Staff/Admin TỰ TAY duyệt (từng bị
    // pipeline flag vi phạm, đưa vào PENDING_REVIEW) với nội dung pipeline TỰ ĐỘNG duyệt vì
    // không vi phạm gì. approvalReviewedBy KHÔNG đủ để phân biệt 1 mình — pipeline cũng tự
    // ghi giá trị actorId hệ thống (xem ContentPipelineServiceImpl.PIPELINE_ACTOR) vào field
    // này khi tự duyệt sạch, nên phải loại trừ đúng giá trị đó mới ra được người thật.
    // Sort lấy từ Pageable (Sort.by approvalReviewedAt DESC ở MediaServiceImpl.listApproved())
    // — không lặp lại ORDER BY tĩnh ở đây, tránh xung đột với Sort động của Pageable.
    @Query("SELECT m FROM Media m WHERE m.approvalStatus = :approvalStatus AND m.status IN :statuses "
            + "AND m.isDeleted = false AND m.approvalReviewedBy IS NOT NULL "
            + "AND m.approvalReviewedBy <> :pipelineActor")
    Page<Media> findManuallyApproved(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.approvalStatus = :approvalStatus AND m.status IN :statuses "
            + "AND m.isDeleted = false "
            + "AND (m.approvalReviewedBy IS NULL OR m.approvalReviewedBy = :pipelineActor)")
    Page<Media> findAutoApproved(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            Pageable pageable);

    // Reconcile fallback cho content pipeline (copyright/kiểm duyệt) bị "mất tích" do gửi
    // Kafka thất bại không đồng bộ — status còn PENDING/HLS_PROCESSING/HLS_READY (chưa
    // terminal) VÀ approvalStatus vẫn PENDING_REVIEW mặc định (chưa có kết luận nào) sau
    // một khoảng lâu bất thường. Không khớp media đã bị flag thật (status=INACTIVE, loại
    // trừ khỏi statuses) hay video đang transcode hợp lệ nhưng đã qua kiểm duyệt xong
    // (approvalStatus đã APPROVED — loại trừ vì filter cứng PENDING_REVIEW).
    List<Media> findAllByStatusInAndApprovalStatusAndUpdatedAtBeforeAndIsDeletedFalseOrderByUpdatedAtAsc(
            Collection<MediaStatus> statuses,
            ContentApprovalStatus approvalStatus,
            LocalDateTime updatedAt,
            Pageable pageable);
}
