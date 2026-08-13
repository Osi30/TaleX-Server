package com.talex.server.services.media.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.kafka.CopyrightResultMessage;
import com.talex.server.dtos.kafka.CopyrightViolationItem;
import com.talex.server.dtos.kafka.ModerationResultMessage;
import com.talex.server.dtos.kafka.ModerationViolationItem;
import com.talex.server.dtos.kafka.PipelineJobMessage;
import com.talex.server.entities.media.ContentCensorship;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaCopyright;
import com.talex.server.entities.media.ViolationDetail;
import com.talex.server.enums.media.CensorshipStatus;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentWarningGroup;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.media.ViolationType;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.dtos.sse.PipelineEventPayload;
import com.talex.server.services.SseNotificationService;
import com.talex.server.services.media.ContentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the content pipeline state machine after media upload completes.
 * Flow: PENDING -> copyright check -> moderation check -> ACTIVE or INACTIVE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentPipelineServiceImpl implements ContentPipelineService {

    private static final String PIPELINE_ACTOR = "content-pipeline";
    private static final String CC0_CODE = "CC0";

    private final MediaRepository mediaRepository;
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final ContentCensorshipRepository contentCensorshipRepository;
    private final ContentPipelineProducer pipelineProducer;
    private final MediaProperties mediaProperties;
    private final SseNotificationService sseNotificationService;
    private final com.talex.server.services.media.MediaPackagingService mediaPackagingService;

    @Override
    public void dispatchPipelineJob(Media media) {
        try {
            // Đã chuyển thành nối tiếp: upload xong -> AI -> HLS, nên status ở đây 
            // chắc chắn chưa phải là HLS_PROCESSING, ta gán thành PENDING để
            // báo hiệu AI đang xử lý.
            if (media.getStatus() != MediaStatus.HLS_PROCESSING) {
                media.setStatus(MediaStatus.PENDING);
            }
            media.markUpdatedBy(PIPELINE_ACTOR);
            mediaRepository.save(media);

            String s3Key = extractS3Key(media.getOriginalUrl(), mediaProperties.getAws().getBucketName());
            PipelineJobMessage message = PipelineJobMessage.builder()
                    .mediaId(media.getMediaId())
                    .s3Key(s3Key)
                    .s3Bucket(mediaProperties.getAws().getBucketName())
                    .mediaType(media.getMediaType().name())
                    .correlationId(UUID.randomUUID().toString())
                    .requestedAt(LocalDateTime.now().toString())
                    .creatorId(resolveEpisodeCreatorId(media))
                    .build();

            pipelineProducer.sendPipelineJob(message);
            log.info("Pipeline job dispatched: mediaId={} type={}", media.getMediaId(), media.getMediaType());
        } catch (Exception e) {
            log.error("Failed to dispatch pipeline job for mediaId={}", media.getMediaId(), e);
            media.setStatus(MediaStatus.FAILED);
            media.setErrorMessage("Pipeline dispatch failed: " + e.getMessage());
            media.markUpdatedBy(PIPELINE_ACTOR);
            mediaRepository.save(media);
        }
    }

    @Override
    @Transactional
    public void handleCopyrightResult(CopyrightResultMessage result) {
        Optional<Media> mediaOpt = mediaRepository.findByMediaIdAndIsDeletedFalse(result.getMediaId());
        if (mediaOpt.isEmpty()) {
            // "Không tìm thấy" có 2 nguyên nhân: (a) media đã bị soft-delete TRONG LÚC job
            // bản quyền đang xử lý (2 topic content-pipeline-job/content-media-delete không
            // đảm bảo thứ tự, lệnh xóa có thể chạy TRƯỚC khi AI kịp insert fingerprint), hoặc
            // (b) mediaId hoàn toàn xa lạ với môi trường này (message từ môi trường khác lẫn
            // vào do trùng topic/group, message cũ bị replay...). Cả 2 case đều KHÔNG dọn
            // Milvus — fingerprint của media đã xóa cố tình được giữ lại để chống re-upload
            // đạo nhái (xem notifyMediaDeleted() không còn được gọi từ MediaServiceImpl.delete()
            // nữa, cùng lý do). Tra thêm 1 lần KHÔNG lọc isDeleted chỉ để log rõ nguyên nhân.
            Optional<Media> anyOpt = mediaRepository.findByMediaId(result.getMediaId());
            if (anyOpt.isPresent() && Boolean.TRUE.equals(anyOpt.get().getIsDeleted())) {
                log.info("Copyright result for soft-deleted mediaId={} — fingerprint intentionally kept in Milvus", result.getMediaId());
            } else {
                log.warn("Copyright result for mediaId={} unknown to this environment — ignoring", result.getMediaId());
            }
            return;
        }
        Media media = mediaOpt.get();

        // Idempotency: skip if already assigned a content ID
        if (media.getContentId() != null) {
            log.info("Copyright result already processed for mediaId={}, skipping", result.getMediaId());
            return;
        }

        media.setContentId(result.getContentId());
        
        if (result.getPreviewS3Key() != null && !result.getPreviewS3Key().isBlank()) {
            String domain = mediaProperties.getAws().getCloudfrontDomain();
            String previewUrl = "https://" + domain + "/" + result.getPreviewS3Key();
            media.setPreviewUrl(previewUrl);
        }

        // watermarkedS3Key rỗng/null nghĩa là bước nhúng watermark đã fail (AI bắt exception,
        // không làm hỏng cả pipeline — xem kafka_consumer_service.py) — ghi lại ĐÚNG kết quả
        // thật, không mặc định true chỉ vì contentId đã có (2 việc độc lập nhau).
        media.setHasWatermark(result.getWatermarkedS3Key() != null && !result.getWatermarkedS3Key().isBlank());
        if (result.getWatermarkedS3Key() != null && !result.getWatermarkedS3Key().isBlank()) {
            String domain = mediaProperties.getAws().getCloudfrontDomain();
            String newUrl = (domain != null && !domain.isBlank())
                    ? "https://" + domain + "/" + result.getWatermarkedS3Key()
                    : "https://" + mediaProperties.getAws().getBucketName() + ".s3." + mediaProperties.getAws().getRegion() + ".amazonaws.com/" + result.getWatermarkedS3Key();
            media.setFileUrl(newUrl);

            // Chỉ ghi đè originalUrl cho IMAGE vì lúc này file là 1 ảnh có watermark.
            // Đối với VIDEO, watermarkedS3Key là 1 folder (videos/ab_hls/{id}),
            // originalUrl BẮT BUỘC phải giữ nguyên là file MP4 gốc để job kiểm duyệt Moderation tải về quét!
            if (media.getMediaType() == MediaType.IMAGE) {
                media.setOriginalUrl(newUrl);
                media.setProviderPublicId(result.getWatermarkedS3Key());
            }
        }

        if (Boolean.FALSE.equals(result.getSuccess())) {
            log.error("Copyright check failed for mediaId={}: {}", result.getMediaId(), result.getErrorMessage());
            media.setStatus(MediaStatus.FAILED);
            media.setApprovalStatus(ContentApprovalStatus.REJECTED);
            media.setApprovalReviewedBy(PIPELINE_ACTOR);
            media.setApprovalReviewedAt(LocalDateTime.now());
            media.setErrorMessage("Copyright check failed: " + result.getErrorMessage());
            media.markUpdatedBy(PIPELINE_ACTOR);
            mediaRepository.save(media);
            pushSseEvent(media, "pipeline:failed", PipelineEventPayload.builder()
                    .mediaId(media.getMediaId()).status("FAILED")
                    .errorMessage(result.getErrorMessage()).failedStep("COPYRIGHT").build());
            return;
        }

        // Từ khi Fingerprint chuyển lên chạy trước Moderation, Moderation LUÔN resolve
        // TRƯỚC Copyright (xem giải thích đầy đủ ở dưới, chỗ moderationAlreadyFlaggedForReview
        // gốc) — nghĩa là nếu Moderation đã flag nội dung (status=INACTIVE), việc CHƯA
        // ĐỤNG vào status là đúng. Nhưng createHlsPackaging() bên dưới set thẳng
        // status=HLS_PROCESSING KHÔNG ĐIỀU KIỆN — nếu gọi vô điều kiện ở đây, nó ghi đè
        // mất INACTIVE ngay lập tức (chạy TRƯỚC cả đoạn check "moderationAlreadyFlaggedForReview"
        // phía dưới), khiến media biến mất khỏi hàng chờ Staff review
        // (MediaRepository.findPendingReviewMedia lọc CHÍNH XÁC status=INACTIVE) dù chưa
        // ai duyệt gì cả — bug thật đã xảy ra, phát hiện qua rà lại code cùng user.
        // Staff vẫn xem trước video được (toAdminPreviewResponse ký originalUrl riêng cho
        // admin, không cần HLS) nên hoãn transcode tới lúc Staff thật sự duyệt không mất
        // chức năng gì — xem MediaServiceImpl.approve() nơi resume packaging này.
        boolean moderationAlreadyFlaggedForReview = media.getApprovalStatus() == ContentApprovalStatus.PENDING_REVIEW
                && !contentCensorshipRepository.findAllByMedia_MediaId(media.getMediaId()).isEmpty();

        // Bắt đầu HLS Packaging (Transcode) TẠI ĐÂY thay vì lúc upload xong.
        // Điều này đảm bảo MediaConvert sử dụng S3 url MỚI (đã có watermark).
        if (!moderationAlreadyFlaggedForReview
                && media.getMediaType() == MediaType.VIDEO && media.getProvider() == MediaProvider.AWS) {
            mediaPackagingService.createHlsPackaging(media);
        }

        if (Boolean.TRUE.equals(result.getIsDuplicate()) && result.getViolations() != null) {
            // INVARIANT: nhánh CC0 dưới đây tin tưởng Copyright.code=="CC0" của media nguồn
            // để bỏ qua hàng chờ Staff review. An toàn CHỈ VÌ Copyright hiện là dữ liệu
            // seed/tham chiếu, chưa có nơi nào cho Creator tự gán bản quyền cho media. Nếu
            // sau này có tính năng gán bản quyền, cờ CC0 phải được Staff xác nhận trước khi
            // được dùng để bỏ qua PENDING_REVIEW ở đây — nếu không, Creator có thể tự khai
            // CC0 để né kiểm tra trùng lặp.
            boolean allViolationsCC0 = processViolations(media, result.getViolations());
            if (!allViolationsCC0) {
                // Máy chỉ biết "2 nội dung giống nhau X%", không biết ai thực sự có quyền —
                // AI service đã tự loại trừ trùng trong cùng creator rồi (nên tới đây chắc
                // chắn là khác creator), nhưng vẫn không đủ căn cứ để tự kết luận "vi phạm"
                // dứt khoát (có thể cả 2 đều hợp pháp, hoặc false positive do thuật toán).
                // Đưa vào hàng chờ Staff review có sẵn thay vì tự động chặn cứng — giống
                // đúng cách YouTube Content ID xử lý case mơ hồ (dispute/review, không phải
                // thuật toán tự phân xử). KHÔNG set reviewedBy/At vì chưa ai review thật.
                log.info("Non-CC0 copyright match: mediaId={} routed to Staff review", result.getMediaId());
                // Không ghi đè nếu transcode đã fail trước đó (chạy song song, có thể fail
                // trước khi copyright check xong) — giữ nguyên FAILED để không mất tín hiệu
                // "video hỏng cần upload lại", tránh Staff hiểu nhầm thành vi phạm nội dung.
                if (media.getStatus() != MediaStatus.FAILED) {
                    media.setStatus(MediaStatus.INACTIVE);
                }
                media.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
                media.markUpdatedBy(PIPELINE_ACTOR);
                mediaRepository.save(media);
                pushSseEvent(media, "pipeline:copyright_complete", PipelineEventPayload.builder()
                        .mediaId(media.getMediaId()).status("COPYRIGHT_COMPLETE")
                        .contentId(result.getContentId()).isDuplicate(true)
                        .violationsCount(result.getViolations().size())
                        .approvalStatus(media.getApprovalStatus().name()).build());
                return;
            }
            log.warn("CC0 auto-approval bypassed Staff review: mediaId={} — sources self-declared CC0", result.getMediaId());
        }

        // moderationAlreadyFlaggedForReview đã tính ở trên (trước đoạn createHlsPackaging)
        // — dùng lại, KHÔNG tính lại lần 2 ở đây (media/censorship không đổi gì thêm giữa
        // 2 điểm này trong cùng 1 lần gọi hàm).
        boolean hlsAlreadyReady = media.getMediaType() != MediaType.VIDEO
                || media.getStatus() == MediaStatus.HLS_READY;
        if (moderationAlreadyFlaggedForReview) {
            log.info("Copyright complete but Moderation already routed media to Staff review — keeping status: mediaId={}",
                    result.getMediaId());
        } else if (hlsAlreadyReady) {
            media.setStatus(MediaStatus.ACTIVE);
            log.info("Pipeline complete — media ACTIVE: mediaId={}", result.getMediaId());
        } else {
            log.info("Pipeline complete, waiting for HLS transcode to finish: mediaId={}", result.getMediaId());
        }
        
        media.markUpdatedBy(PIPELINE_ACTOR);
        mediaRepository.save(media);
        
        pushSseEvent(media, "pipeline:copyright_complete", PipelineEventPayload.builder()
                .mediaId(media.getMediaId()).status("COPYRIGHT_COMPLETE")
                .contentId(result.getContentId()).isDuplicate(false)
                .violationsCount(0).approvalStatus(media.getApprovalStatus().name()).build());
    }

    @Override
    @Transactional
    public void handleModerationResult(ModerationResultMessage result) {
        Optional<Media> mediaOpt = mediaRepository.findByMediaIdAndIsDeletedFalse(result.getMediaId());
        if (mediaOpt.isEmpty()) {
            log.warn("Moderation result received for unknown mediaId={}", result.getMediaId());
            return;
        }
        Media media = mediaOpt.get();

        // Idempotency: skip if censorship record already exists for this media
        List<ContentCensorship> existing = contentCensorshipRepository.findAllByMedia_MediaId(result.getMediaId());
        if (!existing.isEmpty()) {
            log.info("Moderation already processed for mediaId={}, skipping", result.getMediaId());
            return;
        }

        // Lỗi hệ thống (Rekognition timeout, ffmpeg crash...) khác hẳn "nội dung vi phạm thật" —
        // AI service trả isSafe=false kèm success=false trong cả 2 trường hợp, phải phân biệt rõ
        // ở đây, nếu không Creator/Staff nhìn vào tưởng nội dung bị từ chối thật (xem phase-01).
        if (Boolean.FALSE.equals(result.getSuccess())) {
            log.error("Moderation check failed for mediaId={}: {}", result.getMediaId(), result.getErrorMessage());
            media.setStatus(MediaStatus.FAILED);
            media.setErrorMessage("Moderation check failed: " + result.getErrorMessage());
            media.markUpdatedBy(PIPELINE_ACTOR);
            mediaRepository.save(media);
            pushSseEvent(media, "pipeline:failed", PipelineEventPayload.builder()
                    .mediaId(media.getMediaId()).status("FAILED")
                    .errorMessage(result.getErrorMessage()).failedStep("MODERATION").build());
            return;
        }

        ContentCensorship censorship = buildCensorship(media, result);
        contentCensorshipRepository.save(censorship);

        if (Boolean.TRUE.equals(result.getIsSafe())) {
            media.setApprovalStatus(ContentApprovalStatus.APPROVED);
            media.setApprovalReviewedBy(PIPELINE_ACTOR);
            media.setApprovalReviewedAt(LocalDateTime.now());
            // Không cần dispatch gì thêm vì PipelineJob đang chạy trên Python sẽ tự đi tiếp
            return;
        } else if (areAllViolationsShieldedByDeclaredWarnings(media, result.getViolations())) {
            // MỚI: Creator đã khai TRƯỚC đúng nhóm cảnh báo (VD Bạo lực/Máu me) cho TOÀN
            // BỘ nhãn AI phát hiện được, VÀ series đủ 18+ — coi như đã cảnh báo người xem
            // trước, không cần Staff xem lại. Nếu chỉ khớp MỘT PHẦN (còn nhãn nào chưa khai)
            // vẫn rơi xuống nhánh else bên dưới như cũ (xem areAllViolationsShieldedByDeclaredWarnings).
            media.setApprovalStatus(ContentApprovalStatus.APPROVED);
            media.setApprovalReviewedBy(PIPELINE_ACTOR);
            media.setApprovalReviewedAt(LocalDateTime.now());
            log.info("Auto-approved via declared content warnings: mediaId={} label={} seriesAgeRating={}",
                    result.getMediaId(), result.getPrimaryLabel(), resolveSeriesAgeRating(media));
        } else {
            // AI chỉ phát hiện NHÃN nhạy cảm, không tự phán được đây là vi phạm thật hay
            // nội dung hợp lệ theo đúng bối cảnh (series 18+ đã khai, hoặc series CHƯA
            // khai 18+ nhưng nội dung episode này thật sự cần rating đó) — luôn đẩy qua
            // hàng đợi Staff review thay vì tự động từ chối cứng, để tránh chặn oan nội
            // dung hợp lệ chỉ vì Creator quên cập nhật rating series. Staff xem trực tiếp
            // mới quyết định: duyệt (kèm nhắc Creator cập nhật rating nếu cần) hay từ chối
            // thật. KHÔNG set reviewedBy/reviewedAt vì chưa ai thật sự review.
            // Không ghi đè nếu transcode đã FAILED trước đó (xem giải thích ở nhánh non-CC0
            // copyright bên trên) — giữ nguyên tín hiệu "video hỏng" cho Creator/Staff.
            if (media.getStatus() != MediaStatus.FAILED) {
                media.setStatus(MediaStatus.INACTIVE);
            }
            media.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            log.info("Moderation flagged content — routed to Staff review: mediaId={} label={} seriesAgeRating={}",
                    result.getMediaId(), result.getPrimaryLabel(), resolveSeriesAgeRating(media));
        }

        media.markUpdatedBy(PIPELINE_ACTOR);
        mediaRepository.save(media);

        pushSseEvent(media, "pipeline:moderation_complete", PipelineEventPayload.builder()
                .mediaId(media.getMediaId()).status("MODERATION_COMPLETE")
                .isSafe(result.getIsSafe()).primaryLabel(result.getPrimaryLabel())
                .approvalStatus(media.getApprovalStatus().name()).build());
    }

    @Override
    @Transactional
    public void markProcessingFailed(String mediaId, String failedStep, String errorMessage) {
        try {
            Optional<Media> mediaOpt = mediaRepository.findByMediaIdAndIsDeletedFalse(mediaId);
            if (mediaOpt.isEmpty()) {
                log.warn("markProcessingFailed: mediaId={} not found (deleted?), skipping", mediaId);
                return;
            }
            Media media = mediaOpt.get();
            // Xác nhận lại NGAY TRƯỚC KHI ghi — ContentPipelineReconciler tìm ra media
            // "kẹt" qua query rồi mới xử lý tuần tự (có độ trễ), nếu đúng lúc đó kết quả
            // thật đã về (job không hề mất, chỉ đang chậm do dồn tải) và media đã chuyển
            // ACTIVE/INACTIVE, KHÔNG được ghi đè thành FAILED — sẽ làm trạng thái nhảy
            // lung tung dù pipeline đã xong đúng cách.
            boolean stillInFlight = media.getStatus() == MediaStatus.PENDING
                    || media.getStatus() == MediaStatus.HLS_PROCESSING
                    || media.getStatus() == MediaStatus.HLS_READY;
            if (!stillInFlight || media.getApprovalStatus() != ContentApprovalStatus.PENDING_REVIEW) {
                log.info("markProcessingFailed: mediaId={} already resolved (status={}, approvalStatus={}), skipping",
                        mediaId, media.getStatus(), media.getApprovalStatus());
                return;
            }
            media.setStatus(MediaStatus.FAILED);
            media.setErrorMessage(errorMessage);
            media.markUpdatedBy(PIPELINE_ACTOR);
            mediaRepository.save(media);
            pushSseEvent(media, "pipeline:failed", PipelineEventPayload.builder()
                    .mediaId(media.getMediaId()).status("FAILED")
                    .errorMessage(errorMessage).failedStep(failedStep).build());
            log.error("Marked media FAILED after unexpected processing error: mediaId={} step={}", mediaId, failedStep);
        } catch (Exception e) {
            // Best-effort — nếu ngay cả việc đánh dấu FAILED cũng lỗi (DB thật sự down),
            // không còn gì để làm ngoài log rõ ràng cho vận hành viên tra cứu thủ công.
            log.error("Failed to mark media as FAILED after processing error: mediaId={}", mediaId, e);
        }
    }

    @Override
    @Transactional
    public void markStalePipelineFailed(String mediaId, LocalDateTime staleBefore, String errorMessage, String failedStep) {
        int updated = mediaRepository.markStalePipelineFailed(
                mediaId, List.of(MediaStatus.PENDING, MediaStatus.HLS_PROCESSING, MediaStatus.HLS_READY),
                staleBefore, errorMessage, PIPELINE_ACTOR);
        if (updated == 0) {
            // Kết quả thật đã commit đúng lúc UPDATE này chạy — row đã rời khỏi predicate
            // in-flight, WHERE không khớp dòng nào. Không ghi đè, pipeline đã xong đúng cách.
            log.info("Stale-mark no-op for mediaId={} — already resolved concurrently", mediaId);
            return;
        }
        mediaRepository.findByMediaIdAndIsDeletedFalse(mediaId).ifPresent(media ->
                pushSseEvent(media, "pipeline:failed", PipelineEventPayload.builder()
                        .mediaId(mediaId).status("FAILED").errorMessage(errorMessage).failedStep(failedStep).build()));
        log.error("Marked media FAILED after pipeline timeout: mediaId={}", mediaId);
    }

    @Override
    public void notifyMediaDeleted(String mediaId) {
        // Best-effort — dọn fingerprint Milvus mồ côi khi media bị xóa, không được làm
        // fail thao tác xóa chính (đã là entity chính, quan trọng hơn việc dọn dẹp phụ).
        try {
            pipelineProducer.sendMediaDeleted(mediaId);
        } catch (Exception e) {
            log.warn("Failed to notify AI service of media deletion, fingerprint may remain orphaned: mediaId={}", mediaId, e);
        }
    }

    @Override
    public void notifyStaffRejected(Media media, String reason) {
        // Best-effort — creator không nhận được thông báo real-time không phải lỗi nghiêm
        // trọng (họ vẫn thấy được lý do khi tự load lại trang), không được làm fail thao
        // tác từ chối chính.
        try {
            pushSseEvent(media, "pipeline:staff_rejected", PipelineEventPayload.builder()
                    .mediaId(media.getMediaId())
                    .status("REJECTED")
                    .approvalStatus("REJECTED")
                    .reviewerNotes(reason)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to push staff-rejected SSE event: mediaId={}", media.getMediaId(), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Persists MediaCopyright records for each violation.
     * Returns true if ALL violations are from CC0-licensed sources (auto-approvable).
     */
    private boolean processViolations(Media media, List<CopyrightViolationItem> violations) {
        boolean allCC0 = true;
        for (CopyrightViolationItem item : violations) {
            Media sourceMedia = null;
            boolean isCC0 = false;

            Optional<Media> sourceOpt = mediaRepository.findByMediaIdAndIsDeletedFalse(item.getSourceMediaId());
            if (sourceOpt.isPresent()) {
                sourceMedia = sourceOpt.get();
                // Check if source media carries a CC0 license
                isCC0 = sourceMedia.getCopyright() != null
                        && CC0_CODE.equalsIgnoreCase(sourceMedia.getCopyright().getCode());
            }
            // Source not in DB → conservative: treat as non-CC0

            MediaCopyright copyright = new MediaCopyright();
            copyright.setMedia(media);
            copyright.setSourceMedia(sourceMedia);
            copyright.setStartTimeTarget(item.getStartTimeTarget());
            copyright.setEndTimeTarget(item.getEndTimeTarget());
            copyright.setStartTimeSource(item.getStartTimeSource());
            copyright.setEndTimeSource(item.getEndTimeSource());
            copyright.setSimilarityScore(item.getSimilarityScore());
            copyright.setViolationType(parseViolationType(item.getViolationType()));
            copyright.setIsValid(isCC0);
            copyright.setCheckedAt(LocalDateTime.now());
            copyright.markCreatedBy(PIPELINE_ACTOR);
            mediaCopyrightRepository.save(copyright);

            if (!isCC0) {
                allCC0 = false;
            }
        }
        return allCC0;
    }

    private ContentCensorship buildCensorship(Media media, ModerationResultMessage result) {
        ContentCensorship censorship = new ContentCensorship();
        censorship.setMedia(media);
        censorship.setRawResponse(result.getRawResponse());
        censorship.setPrimaryViolationLabel(result.getPrimaryLabel());
        censorship.setConfidenceScore(result.getConfidenceScore());
        censorship.setCheckedAt(LocalDateTime.now());
        censorship.setReviewedBy("AWS_REKOGNITION");
        censorship.setStatus(Boolean.TRUE.equals(result.getIsSafe())
                ? CensorshipStatus.APPROVED : CensorshipStatus.REJECTED);
        censorship.markCreatedBy(PIPELINE_ACTOR);

        if (result.getViolations() != null) {
            for (ModerationViolationItem v : result.getViolations()) {
                ViolationDetail detail = new ViolationDetail();
                detail.setCensorship(censorship);
                detail.setViolationAt(v.getTimestampMs());
                detail.setEndViolationAt(v.getEndTimestampMs());
                detail.setLabel(v.getLabel());
                detail.setParentLabel(v.getParentLabel());
                detail.setConfidence(v.getConfidence());
                detail.setSuggestion(v.getSuggestion());
                detail.markCreatedBy(PIPELINE_ACTOR);
                censorship.getViolationDetails().add(detail);
            }
        }
        return censorship;
    }

    /**
     * Extracts the S3 object key from an S3 URL or CloudFront URL.
     * Handles patterns: s3://bucket/key, https://bucket.s3.region.amazonaws.com/key,
     * https://cloudfront-domain/key
     */
    private String extractS3Key(String url, String bucketName) {
        if (url == null || url.isBlank()) {
            return "";
        }
        // s3://bucket-name/key/path
        if (url.startsWith("s3://")) {
            return url.replaceFirst("s3://" + bucketName + "/", "");
        }
        // https://bucket.s3.region.amazonaws.com/key/path
        String s3Pattern = bucketName + ".s3.";
        int s3Idx = url.indexOf(s3Pattern);
        if (s3Idx >= 0) {
            int slashIdx = url.indexOf('/', s3Idx + s3Pattern.length());
            return slashIdx >= 0 ? url.substring(slashIdx + 1) : "";
        }
        // CloudFront URL: https://domain/key/path — strip protocol+host bằng string
        // parsing thuần thay vì java.net.URI (strict parsing dễ ném URISyntaxException
        // nếu URL chứa ký tự chưa encode đúng chuẩn — trước đây catch() sẽ trả về
        // NGUYÊN VĂN URL đầy đủ làm S3 key, chắc chắn tải S3 thất bại).
        int schemeEnd = url.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = url.indexOf('/', schemeEnd + 3);
            if (pathStart >= 0) {
                return url.substring(pathStart + 1);
            }
        }
        log.warn("Could not parse S3 key from URL: {}", url);
        return url;
    }

    private ViolationType parseViolationType(String raw) {
        if (raw == null) return ViolationType.VIDEO;
        try {
            return ViolationType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ViolationType.VIDEO;
        }
    }

    private void pushSseEvent(Media media, String eventName, PipelineEventPayload payload) {
        String creatorId = resolveCreatorAccountId(media);
        if (creatorId != null) {
            sseNotificationService.pushEvent(creatorId, eventName, payload);
        }
    }

    /**
     * creatorId để AI service loại trừ so khớp Content ID trong cùng creator — không
     * tự báo "vi phạm bản quyền" giữa các tập/trang của chính người đăng (vd. nhân vật
     * lặp lại xuyên suốt 1 bộ truyện). Rỗng nếu chưa gắn episode — an toàn hơn NPE,
     * AI vẫn chạy được, chỉ là không loại trừ được gì trong trường hợp hiếm này.
     */
    private String resolveEpisodeCreatorId(Media media) {
        // Media đã có sẵn field creatorId riêng (denormalized, set ngay lúc tạo — xem
        // DefaultMediaUploadSessionService/MediaServiceImpl) — dùng trực tiếp thay vì đi
        // qua media.getEpisode().getCreatorId() (association LAZY, có rủi ro
        // LazyInitializationException nếu Hibernate session không còn mở đúng lúc gọi,
        // khiến catch() nuốt lỗi và trả về "" — creatorId rỗng làm loại trừ trùng lặp
        // cùng creator KHÔNG BAO GIỜ khớp, gây báo nhầm vi phạm bản quyền với chính mình).
        if (media.getCreatorId() != null && !media.getCreatorId().isBlank()) {
            return media.getCreatorId();
        }
        try {
            return media.getEpisode().getCreatorId();
        } catch (Exception e) {
            log.warn("Could not resolve episode creatorId for media: {}", media.getMediaId());
            return "";
        }
    }

    private String resolveSeriesAgeRating(Media media) {
        try {
            return media.getEpisode().getSeason().getSeries().getAgeRating();
        } catch (Exception e) {
            log.warn("Could not resolve series ageRating for media: {}", media.getMediaId());
            return "";
        }
    }

    private static final String AGE_RATING_MATURE = "MATURE";

    // AWS Rekognition trả ParentName (nhóm L1 gốc) cho từng nhãn con — dùng thẳng field
    // này thay vì tự chế dictionary trùng lặp, để tự đúng khi AWS thêm/đổi taxonomy sau
    // này. Nhãn ở ngay cấp L1 (VD AI trả thẳng "Violence") có parent_name rỗng — fallback
    // kiểm luôn chính label trong trường hợp đó.
    private static final java.util.Map<String, ContentWarningGroup> REKOGNITION_L1_TO_WARNING_GROUP =
            java.util.Map.ofEntries(
                    java.util.Map.entry("Explicit", ContentWarningGroup.SEXUAL_NUDITY),
                    java.util.Map.entry("Non-Explicit Nudity of Intimate parts and Kissing", ContentWarningGroup.SEXUAL_NUDITY),
                    java.util.Map.entry("Swimwear or Underwear", ContentWarningGroup.SEXUAL_NUDITY),
                    java.util.Map.entry("Violence", ContentWarningGroup.VIOLENCE_GORE),
                    java.util.Map.entry("Visually Disturbing", ContentWarningGroup.VIOLENCE_GORE),
                    java.util.Map.entry("Drugs & Tobacco", ContentWarningGroup.DRUGS_TOBACCO),
                    java.util.Map.entry("Alcohol", ContentWarningGroup.ALCOHOL),
                    java.util.Map.entry("Gambling", ContentWarningGroup.GAMBLING),
                    java.util.Map.entry("Hate Symbols", ContentWarningGroup.HATE_SYMBOLS),
                    java.util.Map.entry("Rude Gestures", ContentWarningGroup.RUDE_GESTURES));

    private ContentWarningGroup mapParentLabelToGroup(ModerationViolationItem violation) {
        if (violation.getParentLabel() != null && !violation.getParentLabel().isBlank()) {
            ContentWarningGroup group = REKOGNITION_L1_TO_WARNING_GROUP.get(violation.getParentLabel());
            if (group != null) {
                return group;
            }
        }
        // Fallback: nhãn chính nó đã là cấp L1 (parent_name rỗng khi AWS trả thẳng danh mục gốc)
        return REKOGNITION_L1_TO_WARNING_GROUP.get(violation.getLabel());
    }

    private java.util.Set<ContentWarningGroup> resolveSeriesContentWarnings(Media media) {
        try {
            return media.getEpisode().getSeason().getSeries().getContentWarnings();
        } catch (Exception e) {
            log.warn("Could not resolve series contentWarnings for media: {}", media.getMediaId());
            return java.util.Set.of();
        }
    }

    // Chỉ tự động duyệt khi TOÀN BỘ violation đều thuộc nhóm Series đã khai trước + series
    // đủ 18+ — nếu còn dù chỉ 1 nhãn KHÔNG khớp (chưa khai, hoặc khai nhóm khác), vẫn đẩy
    // Staff review như hành vi gốc để tránh Creator "lách" bằng cách chỉ khai 1 nhóm nhưng
    // đăng nội dung vi phạm nhóm khác.
    private boolean areAllViolationsShieldedByDeclaredWarnings(Media media, List<ModerationViolationItem> violations) {
        if (violations == null || violations.isEmpty()) {
            return false;
        }
        if (!AGE_RATING_MATURE.equals(resolveSeriesAgeRating(media))) {
            return false;
        }
        java.util.Set<ContentWarningGroup> declaredWarnings = resolveSeriesContentWarnings(media);
        if (declaredWarnings.isEmpty()) {
            return false;
        }
        for (ModerationViolationItem violation : violations) {
            ContentWarningGroup group = mapParentLabelToGroup(violation);
            if (group == null || !declaredWarnings.contains(group)) {
                return false;
            }
        }
        return true;
    }

    private String resolveCreatorAccountId(Media media) {
        try {
            return media.getEpisode().getSeason().getSeries().getCreator().getAccount().getAccountId().toString();
        } catch (Exception e) {
            log.warn("Could not resolve creator accountId for media: {}", media.getMediaId());
            return null;
        }
    }
}
