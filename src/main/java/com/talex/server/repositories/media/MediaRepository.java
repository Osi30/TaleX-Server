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

    // Danh sách kiểm duyệt phân trang theo EPISODE, khong theo Media rieng le - 1 episode
    // (VD comic nhieu trang) co the co hang chuc Media cung approvalStatus, phan trang tho
    // theo Media se lam 1 episode bi cat rai ra nhieu trang khac nhau, FE thay episode do
    // "xuat hien lai" o trang sau voi so luong khac (da gap bug nay thuc te). Moi filter
    // gom 2 method: 1) lay trang cac episodeId DUY NHAT (Step 1, dung de phan trang that),
    // 2) lay toan bo Media cua dung batch episodeId do (Step 2, dung o tang service de chon
    // media dai dien + dem so luong that trong tung episode).
    // mediaType nullable - dung nguyen literal null trong JPQL de 1 query xu ly duoc ca
    // "khong loc theo loai" (mediaType = null) lan "loc dung 1 loai" (IMAGE/VIDEO).

    // Tab "Chờ duyệt" — KHÔNG group theo episode (khác các query "Đã duyệt" bên dưới): Duyệt/
    // Từ chối tác động lên TỪNG Media riêng lẻ, group sẽ làm Staff không thấy được các media
    // khác cùng episode cần duyệt riêng. Sort lấy từ Pageable (Sort.by createdAt DESC ở
    // MediaServiceImpl.listPendingReview()).
    // keyword đã được chuẩn hóa "%từ khóa%" thường (lowercase) ở tầng service trước khi
    // truyền vào — so khớp tiêu đề Episode/Season/Series, mediaId, contentId (mã fingerprint
    // hiện trên UI), và tên Creator. creatorId là chuỗi thô trên Media (không phải quan hệ
    // JPA) nên search theo Creator phải qua subquery riêng (SELECT ... FROM Creator c WHERE
    // LOWER(c.account.username) LIKE :keyword) thay vì join thẳng.
    @Query("SELECT m FROM Media m WHERE m.approvalStatus = :approvalStatus AND m.status = :status "
            + "AND m.isDeleted = false AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
            + "AND (:keyword IS NULL "
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
            + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword))")
    Page<Media> findPendingReviewMedia(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("status") MediaStatus status,
            @Param("keyword") String keyword,
            @Param("mediaType") MediaType mediaType,
            Pageable pageable);

    // "Đã duyệt" tab, filter "all" — cho Staff/Admin xem lại nội dung đã duyệt để phát hiện
    // lỡ bấm nhầm, có thể ép ẩn (forceHide) nếu cần. Gồm cả FORCE_HIDDEN để admin thấy những
    // gì mình đã ép ẩn trước đó (và có thể bỏ ép ẩn lại), không chỉ ACTIVE/HLS_READY.
    // keyword: xem giải thích ở findPendingReviewMedia bên trên — match ở đây phải nằm
    // TRONG query xác định trang episodeId (không phải query lấy Media theo episodeIds bên
    // dưới), vì đây mới là query quyết định phân trang thật.
    @Query(value = "SELECT m.episode.episodeId FROM Media m WHERE m.approvalStatus = :approvalStatus "
            + "AND m.status IN :statuses AND m.isDeleted = false AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
            + "AND (:keyword IS NULL "
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
            + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword)) "
            // NULLS LAST tường minh — Postgres mặc định coi NULL là "lớn nhất" nên NULL tự
            // trồi lên ĐẦU trong ORDER BY DESC, làm media chưa từng có approvalReviewedAt
            // (VD dữ liệu cũ/seed) đứng trước cả nội dung vừa duyệt thật, ngược hẳn ý đồ
            // "duyệt gần nhất lên đầu" của tab này.
            + "GROUP BY m.episode.episodeId ORDER BY MAX(m.approvalReviewedAt) DESC NULLS LAST",
            countQuery = "SELECT COUNT(DISTINCT m.episode.episodeId) FROM Media m "
                    + "WHERE m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
                    + "AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
                    + "AND (:keyword IS NULL "
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
                    + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword))")
    Page<String> findDistinctApprovedEpisodeIds(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("mediaType") MediaType mediaType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.episode.episodeId IN :episodeIds "
            + "AND m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
            + "AND (:mediaType IS NULL OR m.mediaType = :mediaType)")
    List<Media> findApprovedMediaByEpisodeIds(
            @Param("episodeIds") Collection<String> episodeIds,
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("mediaType") MediaType mediaType);

    // Filter con của tab "Đã duyệt" — phân biệt nội dung Staff/Admin TỰ TAY duyệt (từng bị
    // pipeline flag vi phạm, đưa vào PENDING_REVIEW) với nội dung pipeline TỰ ĐỘNG duyệt vì
    // không vi phạm gì. approvalReviewedBy KHÔNG đủ để phân biệt 1 mình — pipeline cũng tự
    // ghi giá trị actorId hệ thống (xem ContentPipelineServiceImpl.PIPELINE_ACTOR) vào field
    // này khi tự duyệt sạch, nên phải loại trừ đúng giá trị đó mới ra được người thật.
    @Query(value = "SELECT m.episode.episodeId FROM Media m WHERE m.approvalStatus = :approvalStatus "
            + "AND m.status IN :statuses AND m.isDeleted = false AND m.approvalReviewedBy IS NOT NULL "
            + "AND m.approvalReviewedBy <> :pipelineActor AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
            + "AND (:keyword IS NULL "
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
            + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword)) "
            // NULLS LAST tường minh — Postgres mặc định coi NULL là "lớn nhất" nên NULL tự
            // trồi lên ĐẦU trong ORDER BY DESC, làm media chưa từng có approvalReviewedAt
            // (VD dữ liệu cũ/seed) đứng trước cả nội dung vừa duyệt thật, ngược hẳn ý đồ
            // "duyệt gần nhất lên đầu" của tab này.
            + "GROUP BY m.episode.episodeId ORDER BY MAX(m.approvalReviewedAt) DESC NULLS LAST",
            countQuery = "SELECT COUNT(DISTINCT m.episode.episodeId) FROM Media m "
                    + "WHERE m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
                    + "AND m.approvalReviewedBy IS NOT NULL AND m.approvalReviewedBy <> :pipelineActor "
                    + "AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
                    + "AND (:keyword IS NULL "
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
                    + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword))")
    Page<String> findDistinctManuallyApprovedEpisodeIds(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            @Param("mediaType") MediaType mediaType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.episode.episodeId IN :episodeIds "
            + "AND m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
            + "AND m.approvalReviewedBy IS NOT NULL AND m.approvalReviewedBy <> :pipelineActor "
            + "AND (:mediaType IS NULL OR m.mediaType = :mediaType)")
    List<Media> findManuallyApprovedMediaByEpisodeIds(
            @Param("episodeIds") Collection<String> episodeIds,
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            @Param("mediaType") MediaType mediaType);

    @Query(value = "SELECT m.episode.episodeId FROM Media m WHERE m.approvalStatus = :approvalStatus "
            + "AND m.status IN :statuses AND m.isDeleted = false "
            + "AND (m.approvalReviewedBy IS NULL OR m.approvalReviewedBy = :pipelineActor) "
            + "AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
            + "AND (:keyword IS NULL "
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
            + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword)) "
            // NULLS LAST tường minh — Postgres mặc định coi NULL là "lớn nhất" nên NULL tự
            // trồi lên ĐẦU trong ORDER BY DESC, làm media chưa từng có approvalReviewedAt
            // (VD dữ liệu cũ/seed) đứng trước cả nội dung vừa duyệt thật, ngược hẳn ý đồ
            // "duyệt gần nhất lên đầu" của tab này.
            + "GROUP BY m.episode.episodeId ORDER BY MAX(m.approvalReviewedAt) DESC NULLS LAST",
            countQuery = "SELECT COUNT(DISTINCT m.episode.episodeId) FROM Media m "
                    + "WHERE m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
                    + "AND (m.approvalReviewedBy IS NULL OR m.approvalReviewedBy = :pipelineActor) "
                    + "AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
                    + "AND (:keyword IS NULL "
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
                    + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword))")
    Page<String> findDistinctAutoApprovedEpisodeIds(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            @Param("mediaType") MediaType mediaType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.episode.episodeId IN :episodeIds "
            + "AND m.approvalStatus = :approvalStatus AND m.status IN :statuses AND m.isDeleted = false "
            + "AND (m.approvalReviewedBy IS NULL OR m.approvalReviewedBy = :pipelineActor) "
            + "AND (:mediaType IS NULL OR m.mediaType = :mediaType)")
    List<Media> findAutoApprovedMediaByEpisodeIds(
            @Param("episodeIds") Collection<String> episodeIds,
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("statuses") Collection<MediaStatus> statuses,
            @Param("pipelineActor") String pipelineActor,
            @Param("mediaType") MediaType mediaType);

    // Tab "Từ chối" — gồm cả bị Staff từ chối tay (rejectWithReason) lẫn pipeline tự động
    // reject do lỗi hệ thống (VD copyright check fail, xem ContentPipelineServiceImpl).
    // KHÔNG lọc theo MediaStatus (khác tab "Đã duyệt" dùng APPROVED_TAB_STATUSES cố định) —
    // media bị từ chối có thể đang ở nhiều status khác nhau tùy trạng thái trước đó
    // (FAILED/HIDDEN/INACTIVE...), lọc cứng sẽ bỏ sót nội dung thật sự bị từ chối.
    @Query(value = "SELECT m.episode.episodeId FROM Media m WHERE m.approvalStatus = :approvalStatus "
            + "AND m.isDeleted = false AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
            + "AND (:keyword IS NULL "
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
            + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
            + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword "
            + "     OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword)) "
            + "GROUP BY m.episode.episodeId ORDER BY MAX(m.approvalReviewedAt) DESC NULLS LAST",
            countQuery = "SELECT COUNT(DISTINCT m.episode.episodeId) FROM Media m "
                    + "WHERE m.approvalStatus = :approvalStatus AND m.isDeleted = false "
                    + "AND (:mediaType IS NULL OR m.mediaType = :mediaType) "
                    + "AND (:keyword IS NULL "
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.title)) AS string) LIKE :keyword"
                    + "     OR CAST(FUNCTION('unaccent', LOWER(m.episode.season.series.title)) AS string) LIKE :keyword"
                    + "     OR LOWER(m.mediaId) LIKE :keyword OR LOWER(m.contentId) LIKE :keyword "
                    + "     OR m.creatorId IN (SELECT c.creatorId FROM Creator c WHERE LOWER(c.account.username) LIKE :keyword))")
    Page<String> findDistinctRejectedEpisodeIds(
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("mediaType") MediaType mediaType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.episode.episodeId IN :episodeIds "
            + "AND m.approvalStatus = :approvalStatus AND m.isDeleted = false "
            + "AND (:mediaType IS NULL OR m.mediaType = :mediaType)")
    List<Media> findRejectedMediaByEpisodeIds(
            @Param("episodeIds") Collection<String> episodeIds,
            @Param("approvalStatus") ContentApprovalStatus approvalStatus,
            @Param("mediaType") MediaType mediaType);

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
