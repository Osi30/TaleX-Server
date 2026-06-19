package com.talex.server.services.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.entities.Media;
import com.talex.server.enums.MediaStatus;
import com.talex.server.repositories.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import org.springframework.data.domain.Pageable;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

/**
 * Polls SQS queue for MediaConvert job completion events.
 * Updates Media status to HLS_READY or FAILED based on event.
 *
 * Pattern mirrors CloudinaryHlsReconcileService but uses SQS push
 * (via EventBridge) instead of polling Cloudinary Admin API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SqsMediaEventPoller {

    private static final String RECONCILE_ACTOR = "aws-mediaconvert-event";

    private final SqsClient sqsClient;
    private final MediaRepository mediaRepository;
    private final MediaProperties mediaProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 30_000) // poll every 30 seconds
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

                // Delete from queue after processing (even if failed, to avoid infinite retry)
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (Exception e) {
            log.warn("SQS poll failed: {}", e.getMessage());
        }
    }

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
            handleJobFailed(root, jobId, status);
        }
    }

    private void handleJobComplete(JsonNode root, String jobId) {
        // Extract output path to find .m3u8 URL
        String hlsUrl = extractHlsUrl(root);
        if (hlsUrl == null) {
            log.warn("MediaConvert COMPLETE but no HLS output found. jobId={}", jobId);
            return;
        }

        // Find media by checking jobId — MediaConvert doesn't pass mediaId directly,
        // but output path pattern contains episodeId/mediaId.
        // We find media by searching for HLS_PROCESSING entries for this job.
        List<Media> processingMedia = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        com.talex.server.enums.MediaProvider.AWS,
                        List.of(MediaStatus.HLS_PROCESSING),
                        java.time.LocalDateTime.now().plusHours(1),
                        Pageable.unpaged());

        for (Media media : processingMedia) {
            // Check if this media's expected HLS URL matches the completed job output
            String expectedPrefix = "output/videos/"
                    + media.getEpisode().getEpisodeId() + "/"
                    + media.getMediaId() + "/hls/";
            if (hlsUrl.contains(expectedPrefix) || processingMedia.size() == 1) {
                markHlsReady(media, hlsUrl);
                log.info("Media HLS_READY via SQS. mediaId={} jobId={}", media.getMediaId(), jobId);
                return;
            }
        }

        log.info("No matching media found for MediaConvert job. jobId={} hlsUrl={}", jobId, hlsUrl);
    }

    private void handleJobFailed(JsonNode root, String jobId, String status) {
        List<Media> processingMedia = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        com.talex.server.enums.MediaProvider.AWS,
                        List.of(MediaStatus.HLS_PROCESSING),
                        java.time.LocalDateTime.now().plusHours(1),
                        Pageable.unpaged());

        for (Media media : processingMedia) {
            if (media.getMediaId() != null) {
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
        // EventBridge detail contains outputGroupDetails with outputFilePaths
        JsonNode outputGroups = root.path("detail").path("outputGroupDetails");
        if (outputGroups.isArray()) {
            for (JsonNode group : outputGroups) {
                String type = group.path("type").asText();
                if ("HLS_GROUP".equals(type)) {
                    JsonNode playlistPaths = group.path("playlistFilePaths");
                    if (playlistPaths.isArray() && playlistPaths.size() > 0) {
                        // S3 URL: s3://bucket/output/.../master.m3u8
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

        // s3://bucket-name/key → /key
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
