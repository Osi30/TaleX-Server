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
            media.setStatus(MediaStatus.PENDING);
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
                log.warn("Non-CC0 copyright violation: mediaId={} blocked", result.getMediaId());
                media.setStatus(MediaStatus.INACTIVE);
                media.setApprovalStatus(ContentApprovalStatus.REJECTED);
                media.setApprovalReviewedBy(PIPELINE_ACTOR);
                media.setApprovalReviewedAt(LocalDateTime.now());
                media.markUpdatedBy(PIPELINE_ACTOR);
                mediaRepository.save(media);
                pushSseEvent(media, "pipeline:copyright_complete", PipelineEventPayload.builder()
                        .mediaId(media.getMediaId()).status("COPYRIGHT_COMPLETE")
                        .contentId(result.getContentId()).isDuplicate(true)
                        .violationsCount(result.getViolations().size()).build());
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

        ContentCensorship censorship = buildCensorship(media, result);
        contentCensorshipRepository.save(censorship);

        if (Boolean.TRUE.equals(result.getIsSafe())) {
            media.setStatus(MediaStatus.ACTIVE);
            media.setApprovalStatus(ContentApprovalStatus.APPROVED);
            log.info("Moderation passed — media ACTIVE: mediaId={}", result.getMediaId());
        } else {
            media.setStatus(MediaStatus.INACTIVE);
            media.setApprovalStatus(ContentApprovalStatus.REJECTED);
            log.warn("Moderation failed — media INACTIVE: mediaId={} label={}", result.getMediaId(), result.getPrimaryLabel());
        }

        media.setApprovalReviewedBy(PIPELINE_ACTOR);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.markUpdatedBy(PIPELINE_ACTOR);
        mediaRepository.save(media);

        pushSseEvent(media, "pipeline:moderation_complete", PipelineEventPayload.builder()
                .mediaId(media.getMediaId()).status("MODERATION_COMPLETE")
                .isSafe(result.getIsSafe()).primaryLabel(result.getPrimaryLabel()).build());
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

    private String resolveCreatorAccountId(Media media) {
        try {
            return media.getEpisode().getSeason().getSeries().getCreator().getAccount().getAccountId().toString();
        } catch (Exception e) {
            log.warn("Could not resolve creator accountId for media: {}", media.getMediaId());
            return null;
        }
    }
}
