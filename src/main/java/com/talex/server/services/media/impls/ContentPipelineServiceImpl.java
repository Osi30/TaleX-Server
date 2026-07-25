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

    @Override
    public void dispatchPipelineJob(Media media) {
        try {
            // VIDEO: job này giờ dispatch SONG SONG lúc MediaConvert đang transcode (xem
            // DefaultMediaUploadSessionService.complete()) — không được ghi đè HLS_PROCESSING,
            // nếu không SqsMediaEventPoller sẽ không biết transcode đang chạy dở.
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
            log.warn("Copyright result received for unknown mediaId={}", result.getMediaId());
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

        if (Boolean.TRUE.equals(result.getIsDuplicate()) && result.getViolations() != null) {
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
            log.info("All violations are CC0 — auto-approved, proceeding to moderation: mediaId={}", result.getMediaId());
        }

        // No blocking violation — trigger moderation
        dispatchModerationJob(media, result.getCorrelationId());
        media.markUpdatedBy(PIPELINE_ACTOR);
        mediaRepository.save(media);
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
            // VIDEO có thể vẫn đang transcode dở (chạy song song, xem dispatchPipelineJob) —
            // chỉ chuyển ACTIVE khi HLS thật sự đã sẵn sàng, nếu không video sẽ lỗi vì chưa
            // có file phát được. Nếu chưa xong, giữ nguyên HLS_PROCESSING — SqsMediaEventPoller
            // sẽ tự chuyển ACTIVE khi transcode xong (thấy ApprovalStatus đã APPROVED sẵn).
            // Dùng MediaStatus chứ không dùng hlsUrl — hlsUrl bị set NGAY lúc upload xong
            // (URL dự đoán, xem S3MediaProviderService.applyCompletedUpload()), không phải
            // khi transcode thật sự hoàn tất. Chỉ coi "đã sẵn sàng" khi status ĐÚNG LÀ
            // HLS_READY (markHlsReady() đã xác nhận transcode xong) — không dùng kiểu phủ
            // định "!= HLS_PROCESSING", vì nếu transcode đã FAILED thì điều kiện đó cũng
            // đúng, sẽ set nhầm ACTIVE đè lên FAILED.
            boolean hlsAlreadyReady = media.getMediaType() != MediaType.VIDEO
                    || media.getStatus() == MediaStatus.HLS_READY;
            if (hlsAlreadyReady) {
                media.setStatus(MediaStatus.ACTIVE);
                log.info("Moderation passed — media ACTIVE: mediaId={}", result.getMediaId());
            } else {
                log.info("Moderation passed, waiting for HLS transcode to finish: mediaId={}", result.getMediaId());
            }
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
    public void notifyMediaDeleted(String mediaId) {
        // Best-effort — dọn fingerprint Milvus mồ côi khi media bị xóa, không được làm
        // fail thao tác xóa chính (đã là entity chính, quan trọng hơn việc dọn dẹp phụ).
        try {
            pipelineProducer.sendMediaDeleted(mediaId);
        } catch (Exception e) {
            log.warn("Failed to notify AI service of media deletion, fingerprint may remain orphaned: mediaId={}", mediaId, e);
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

    private void dispatchModerationJob(Media media, String correlationId) {
        PipelineJobMessage message = PipelineJobMessage.builder()
                .mediaId(media.getMediaId())
                .s3Key(extractS3Key(media.getOriginalUrl(), mediaProperties.getAws().getBucketName()))
                .s3Bucket(mediaProperties.getAws().getBucketName())
                .mediaType(media.getMediaType().name())
                .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .requestedAt(LocalDateTime.now().toString())
                // Bắt buộc phải set — Python PipelineJobMessage.creator_id là str (default
                // rỗng chỉ áp dụng khi field THIẾU HẲN trong JSON). Nếu không set, Java
                // serialize thành "creatorId": null tường minh, Pydantic validate null vào
                // str sẽ FAIL toàn bộ job trước khi kịp gửi kết quả lỗi về — job coi như
                // mất tích vĩnh viễn.
                .creatorId(resolveEpisodeCreatorId(media))
                .build();
        pipelineProducer.sendModerationJob(message);
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
        // CloudFront URL: https://domain/key/path — strip protocol+host
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("Could not parse S3 key from URL: {}", url);
            return url;
        }
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

    private String resolveCreatorAccountId(Media media) {
        try {
            return media.getEpisode().getSeason().getSeries().getCreator().getAccount().getAccountId().toString();
        } catch (Exception e) {
            log.warn("Could not resolve creator accountId for media: {}", media.getMediaId());
            return null;
        }
    }
}
