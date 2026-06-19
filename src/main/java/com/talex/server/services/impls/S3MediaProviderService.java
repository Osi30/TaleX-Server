package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.entities.Media;
import com.talex.server.entities.MediaUploadSession;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.SignedUploadParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3MediaProviderService implements MediaProviderService, MediaPackagingService {

    private final MediaProperties mediaProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaConvertService mediaConvertService;

    @Override
    public SignedUploadParams createSignedUploadParams(String providerPublicId, String providerDeliveryType) {
        MediaProperties.Aws aws = mediaProperties.getAws();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(aws.getBucketName())
                .key(providerPublicId)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        Map<String, String> uploadParams = new LinkedHashMap<>();
        uploadParams.put("bucket", aws.getBucketName());
        uploadParams.put("key", providerPublicId);
        uploadParams.put("region", aws.getRegion());

        log.info("S3 presigned upload URL created. bucket={} key={}", aws.getBucketName(), providerPublicId);

        return SignedUploadParams.builder()
                .uploadUrl(presignedRequest.url().toString())
                .uploadParams(uploadParams)
                .build();
    }

    @Override
    public String buildVideoPublicId(String episodeId, String mediaId) {
        String env = sanitize(mediaProperties.getAppEnv());
        String episode = sanitize(episodeId);
        String media = sanitize(mediaId);
        return String.format("source/videos/%s/%s/%s/original.mp4", env, episode, media);
    }

    @Override
    public void applyCompletedUpload(Media media, MediaUploadSession session, MediaUploadCompleteRequestDto request) {
        MediaProperties.Aws aws = mediaProperties.getAws();
        String cloudfrontDomain = aws.getCloudfrontDomain();
        String key = request.getPublicId();

        String baseUrl;
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            baseUrl = "https://" + cloudfrontDomain + "/" + key;
        } else {
            baseUrl = "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + key;
        }

        media.setProvider(MediaProvider.AWS);
        media.setProviderAssetId(blankToNull(request.getAssetId()));
        media.setProviderPublicId(key);
        media.setProviderDeliveryType(null);
        // Build predicted HLS output path (will be confirmed by MediaConvert event)
        String episodeId = media.getEpisode().getEpisodeId();
        String mediaId = media.getMediaId();
        String hlsOutputKey = String.format("output/videos/%s/%s/hls/master.m3u8", episodeId, mediaId);

        String predictedHlsUrl;
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            predictedHlsUrl = "https://" + cloudfrontDomain + "/" + hlsOutputKey;
        } else {
            predictedHlsUrl = "https://" + aws.getBucketName() + ".s3." + aws.getRegion()
                    + ".amazonaws.com/" + hlsOutputKey;
        }

        media.setOriginalUrl(baseUrl);
        media.setFileUrl(baseUrl);
        media.setPlaybackUrl(predictedHlsUrl);
        media.setHlsUrl(predictedHlsUrl);
        media.setThumbnailUrl(null);
        media.setFormat(blankToNull(request.getFormat()));
        media.setMimeType(session.getMimeType());
        media.setFileSize(request.getBytes());
        media.setWidth(request.getWidth());
        media.setHeight(request.getHeight());
        media.setDuration(request.getDuration() == null ? null : Math.round(request.getDuration()));
        media.setResolution(request.getWidth() != null && request.getHeight() != null
                ? request.getWidth() + "x" + request.getHeight()
                : null);
        media.setChecksum(sha256(key + ":" + request.getBytes()));
        media.setMediaType(MediaType.VIDEO);
        media.setStatus(MediaStatus.HLS_PROCESSING);
        media.setErrorMessage(null);
        media.setPendingDelete(false);

        // Submit MediaConvert HLS transcode job (async)
        try {
            String jobId = mediaConvertService.submitHlsJob(media);
            log.info("MediaConvert HLS job submitted for S3 upload. mediaId={} jobId={}",
                    media.getMediaId(), jobId);
        } catch (Exception e) {
            log.error("Failed to submit MediaConvert job, falling back to direct MP4. mediaId={}",
                    media.getMediaId(), e);
            media.setStatus(MediaStatus.ACTIVE);
            media.setPlaybackUrl(baseUrl);
            media.setHlsUrl(null);
        }
    }

    @Override
    public String buildHlsUrl(Media media) {
        // Return predicted HLS URL (same as stored in applyCompletedUpload)
        // SqsMediaEventPoller will update to actual CloudFront HLS URL
        if (media.getHlsUrl() != null && !media.getHlsUrl().isBlank()) {
            return media.getHlsUrl();
        }
        String cloudfrontDomain = mediaProperties.getAws().getCloudfrontDomain();
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            String hlsKey = String.format("output/videos/%s/%s/hls/master.m3u8",
                    media.getEpisode().getEpisodeId(), media.getMediaId());
            return "https://" + cloudfrontDomain + "/" + hlsKey;
        }
        return null;
    }

    @Override
    public String buildSignedHlsUrl(Media media, LocalDateTime expiresAt) {
        return media.getFileUrl();
    }

    @Override
    public String buildThumbnailUrl(Media media) {
        return media.getThumbnailUrl();
    }

    @Override
    public void deleteAsset(Media media) {
        String key = media.getProviderPublicId();
        if (key == null || key.isBlank()) {
            log.warn("S3 delete skipped: no providerPublicId for mediaId={}", media.getMediaId());
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(mediaProperties.getAws().getBucketName())
                .key(key)
                .build());
        log.info("S3 asset deleted. mediaId={} key={}", media.getMediaId(), key);
    }

    @Override
    public String createHlsPackaging(Media media) {
        // Trigger MediaConvert HLS transcode for existing media
        try {
            String jobId = mediaConvertService.submitHlsJob(media);
            media.setStatus(MediaStatus.HLS_PROCESSING);
            log.info("MediaConvert HLS job submitted. mediaId={} jobId={}", media.getMediaId(), jobId);
            return jobId;
        } catch (Exception e) {
            log.error("Failed to submit MediaConvert job for mediaId={}", media.getMediaId(), e);
            return null;
        }
    }

    @Override
    public String createDashPackaging(Media media) {
        return null;
    }

    @Override
    public String getManifestUrl(Media media) {
        return media.getFileUrl();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
