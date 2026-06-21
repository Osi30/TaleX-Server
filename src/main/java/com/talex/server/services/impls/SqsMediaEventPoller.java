package com.talex.server.services.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.entities.Media;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.repositories.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.GetJobRequest;
import software.amazon.awssdk.services.mediaconvert.model.GetJobResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Two-layer reliability for MediaConvert job completion:
 * 1. Primary: SQS event poll (EventBridge → SQS) every 30s
 * 2. Fallback: Direct MediaConvert job status poll every 60s for stale entries
 *    (mirrors CloudinaryHlsReconcileService pattern)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SqsMediaEventPoller {

    private static final String RECONCILE_ACTOR = "aws-mediaconvert-event";

    private final SqsClient sqsClient;
    private final MediaConvertClient mediaConvertClient;
    private final MediaRepository mediaRepository;
    private final MediaProperties mediaProperties;
    private final ObjectMapper objectMapper;

    // ── Primary: SQS event-driven notification ────────────────────────────────

    @Scheduled(fixedDelay = 30_000)
    public void pollSqsMessages() {
        MediaProperties.Aws aws = mediaProperties.getAws();
        String queueUrl = aws.getSqsQueueUrl();

        if (queueUrl == null || queueUrl.isBlank()) {
            return;
        }

        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)
                            .build());

            List<Message> messages = response.messages();
            if (messages.isEmpty()) {
                return;
            }

            log.debug("SQS poll: {} messages received", messages.size());

            for (Message message : messages) {
                try {
                    processEvent(message);
                } catch (Exception e) {
                    log.error("Failed to process SQS message. messageId={}", message.messageId(), e);
                }

                // Delete after processing to avoid infinite retry (DLQ handles poison messages)
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception e) {
            log.warn("SQS poll failed: {}", e.getMessage());
        }
    }

    // ── Fallback: Direct MediaConvert job status poll ─────────────────────────

    /**
     * Reconcile fallback: directly polls MediaConvert for any HLS_PROCESSING media
     * that hasn't been updated in >2 minutes. Catches cases where SQS message was lost.
     * Mirrors CloudinaryHlsReconcileService pattern.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reconcileStaleHlsJobs() {
        // Find AWS media stuck in HLS_PROCESSING for more than 2 minutes
        List<Media> stale = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        MediaProvider.AWS,
                        List.of(MediaStatus.HLS_PROCESSING),
                        LocalDateTime.now().minusMinutes(2),
                        Pageable.ofSize(20));

        if (stale.isEmpty()) {
            return;
        }

        log.debug("Reconcile: {} stale HLS_PROCESSING media found", stale.size());

        for (Media media : stale) {
            try {
                reconcileMedia(media);
            } catch (Exception e) {
                log.warn("Reconcile failed for mediaId={}", media.getMediaId(), e);
            }
        }
    }

    private void reconcileMedia(Media media) {
        String jobId = media.getProviderAssetId(); // stored in applyCompletedUpload()

        if (jobId == null || jobId.isBlank()) {
            // No job ID stored — if stuck >30 minutes, mark as failed
            if (media.getUpdatedAt() != null
                    && media.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
                media.setStatus(MediaStatus.FAILED);
                media.setErrorMessage("HLS transcode timed out: no MediaConvert job ID found");
                media.markUpdatedBy("aws-reconcile");
                mediaRepository.save(media);
                log.error("Reconcile: timed out with no jobId. mediaId={}", media.getMediaId());
            }
            return;
        }

        GetJobResponse job = mediaConvertClient.getJob(
                GetJobRequest.builder().id(jobId).build());
        String status = job.job().statusAsString();

        if ("COMPLETE".equals(status)) {
            markHlsReady(media, media.getHlsUrl());
            log.info("Reconcile: HLS_READY via direct poll. mediaId={} jobId={}", media.getMediaId(), jobId);
        } else if ("ERROR".equals(status) || "CANCELED".equals(status)) {
            media.setStatus(MediaStatus.FAILED);
            media.setErrorMessage("MediaConvert job " + status + " (reconcile)");
            media.markUpdatedBy("aws-reconcile");
            mediaRepository.save(media);
            log.error("Reconcile: job failed. mediaId={} jobId={} status={}", media.getMediaId(), jobId, status);
        }
        // SUBMITTED/PROGRESSING → still running, skip
    }

    // ── Event processing ──────────────────────────────────────────────────────

    private void processEvent(Message message) {
        String body = message.body();
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("SQS message is not valid JSON, skipping. messageId={}", message.messageId());
            return;
        }

        String detailType = root.path("detail-type").asText();
        if (!"MediaConvert Job State Change".equals(detailType)) {
            return;
        }

        String status = root.path("detail").path("status").asText();
        String jobId = root.path("detail").path("jobId").asText();

        if ("COMPLETE".equals(status)) {
            handleJobComplete(root, jobId);
        } else if ("ERROR".equals(status) || "CANCELED".equals(status)) {
            handleJobFailed(jobId, status);
        }
    }

    private void handleJobComplete(JsonNode root, String jobId) {
        String hlsUrl = extractHlsUrl(root);
        if (hlsUrl == null) {
            log.warn("MediaConvert COMPLETE but no HLS output found. jobId={}", jobId);
            return;
        }

        // Match by jobId stored in providerAssetId (set during applyCompletedUpload)
        List<Media> processingMedia = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        MediaProvider.AWS,
                        List.of(MediaStatus.HLS_PROCESSING),
                        LocalDateTime.now().plusHours(1),
                        Pageable.unpaged());

        for (Media media : processingMedia) {
            boolean matchByJobId = jobId.equals(media.getProviderAssetId());
            String expectedPrefix = "output/videos/"
                    + media.getEpisode().getEpisodeId() + "/"
                    + media.getMediaId() + "/hls/";
            boolean matchByPath = hlsUrl.contains(expectedPrefix);

            if (matchByJobId || matchByPath || processingMedia.size() == 1) {
                markHlsReady(media, hlsUrl);
                log.info("Media HLS_READY via SQS. mediaId={} jobId={}", media.getMediaId(), jobId);
                return;
            }
        }

        log.info("No matching media found for MediaConvert job. jobId={}", jobId);
    }

    private void handleJobFailed(String jobId, String status) {
        List<Media> processingMedia = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        MediaProvider.AWS,
                        List.of(MediaStatus.HLS_PROCESSING),
                        LocalDateTime.now().plusHours(1),
                        Pageable.unpaged());

        for (Media media : processingMedia) {
            boolean matchByJobId = jobId.equals(media.getProviderAssetId());
            if (matchByJobId || processingMedia.size() == 1) {
                media.setStatus(MediaStatus.FAILED);
                media.setErrorMessage("MediaConvert job " + status + ": " + jobId);
                media.markUpdatedBy(RECONCILE_ACTOR);
                mediaRepository.save(media);
                log.error("Media HLS failed via SQS. mediaId={} jobId={} status={}",
                        media.getMediaId(), jobId, status);
                return;
            }
        }
    }

    private String extractHlsUrl(JsonNode root) {
        JsonNode outputGroups = root.path("detail").path("outputGroupDetails");
        if (outputGroups.isArray()) {
            for (JsonNode group : outputGroups) {
                String type = group.path("type").asText();
                if ("HLS_GROUP".equals(type)) {
                    JsonNode playlistPaths = group.path("playlistFilePaths");
                    if (playlistPaths.isArray() && !playlistPaths.isEmpty()) {
                        String s3Url = playlistPaths.get(0).asText();
                        return convertToCloudFrontUrl(s3Url);
                    }
                }
            }
        }
        return null;
    }

    private String convertToCloudFrontUrl(String s3Url) {
        MediaProperties.Aws aws = mediaProperties.getAws();
        String cloudfrontDomain = aws.getCloudfrontDomain();
        String key = s3Url.replaceFirst("s3://" + aws.getBucketName() + "/", "");

        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            return "https://" + cloudfrontDomain + "/" + key;
        }
        return "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + key;
    }

    private void markHlsReady(Media media, String hlsUrl) {
        String url = StringUtils.hasText(hlsUrl) ? hlsUrl : media.getHlsUrl();
        media.setHlsUrl(url);
        media.setPlaybackUrl(url);
        media.setStatus(MediaStatus.HLS_READY);
        media.setErrorMessage(null);
        media.markUpdatedBy(RECONCILE_ACTOR);
        mediaRepository.save(media);
    }
}
