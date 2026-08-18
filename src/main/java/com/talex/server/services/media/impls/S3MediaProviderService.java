package com.talex.server.services.media.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.media.ImagePresignedUploadRequestDto;
import com.talex.server.dtos.requests.media.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.responses.media.ImagePresignedUploadResponseDto;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaUploadSession;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaProviderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3MediaProviderService implements MediaProviderService, MediaPackagingService {

    private final MediaProperties mediaProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaConvertService mediaConvertService;
    private final CloudFrontUtilities cloudFrontUtilities;

    // Cached temp file holding the CloudFront private key PEM — initialized once at startup
    private volatile Path cloudFrontKeyFile;

    @PostConstruct
    private void initCloudFrontKey() {
        String base64Key = mediaProperties.getAws().getCloudfrontPrivateKey();
        if (base64Key == null || base64Key.isBlank()) {
            log.info("CloudFront private key not configured — signed URLs disabled, unsigned URLs will be used");
            return;
        }
        try {
            byte[] pemBytes = Base64.getDecoder().decode(base64Key);
            cloudFrontKeyFile = Files.createTempFile("cf-pk-", ".pem");
            Files.write(cloudFrontKeyFile, pemBytes);
            cloudFrontKeyFile.toFile().deleteOnExit(); // cleanup on JVM shutdown
            log.info("CloudFront private key loaded — signed URL signing enabled");
        } catch (Exception e) {
            log.warn("CloudFront private key init failed, signing disabled: {}", e.getMessage());
        }
    }

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
    public ImagePresignedUploadResponseDto createImagePresignedUpload(ImagePresignedUploadRequestDto request) {
        MediaProperties.Aws aws = mediaProperties.getAws();
        String env = sanitize(mediaProperties.getAppEnv());
        String context = sanitize(request.getImageContext());
        String ext = extractExtension(request.getFileName());
        String key = String.format("images/%s/%s/%s.%s", env, context, java.util.UUID.randomUUID(), ext);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(aws.getBucketName())
                .key(key)
                .contentType(request.getMimeType())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .putObjectRequest(objectRequest)
                        .build());

        String cloudfrontDomain = aws.getCloudfrontDomain();
        String publicUrl = (cloudfrontDomain != null && !cloudfrontDomain.isBlank())
                ? "https://" + cloudfrontDomain + "/" + key
                : "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + key;

        log.info("S3 image presigned URL created. context={} key={}", context, key);

        return ImagePresignedUploadResponseDto.builder()
                .uploadUrl(presignedRequest.url().toString())
                .key(key)
                .publicUrl(publicUrl)
                .bucket(aws.getBucketName())
                .region(aws.getRegion())
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

        // Build CloudFront/S3 URL for the raw MP4
        String baseUrl;
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            baseUrl = "https://" + cloudfrontDomain + "/" + key;
        } else {
            baseUrl = "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + key;
        }

        media.setProvider(MediaProvider.AWS);
        media.setProviderPublicId(key);
        media.setProviderDeliveryType(null);

        String episodeId = media.getEpisode().getEpisodeId();
        String mediaId = media.getMediaId();

        // Predicted HLS master manifest URL (MediaConvert writes to this path)
        String hlsOutputKey = String.format("output/videos/%s/%s/hls/master.m3u8", episodeId, mediaId);
        String predictedHlsUrl = (cloudfrontDomain != null && !cloudfrontDomain.isBlank())
                ? "https://" + cloudfrontDomain + "/" + hlsOutputKey
                : "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + hlsOutputKey;

        // Predicted thumbnail URL (MediaConvert FRAME_CAPTURE: {inputName}{nameModifier}.{index}.jpg)
        String thumbnailKey = String.format("output/videos/%s/%s/thumbnails/original_thumb.0000000.jpg", episodeId, mediaId);
        String predictedThumbnailUrl = (cloudfrontDomain != null && !cloudfrontDomain.isBlank())
                ? "https://" + cloudfrontDomain + "/" + thumbnailKey
                : "https://" + aws.getBucketName() + ".s3." + aws.getRegion() + ".amazonaws.com/" + thumbnailKey;

        media.setOriginalUrl(baseUrl);
        media.setFileUrl(baseUrl);
        media.setPlaybackUrl(predictedHlsUrl);
        media.setHlsUrl(predictedHlsUrl);
        media.setThumbnailUrl(predictedThumbnailUrl);
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
        // Thay vì HLS_PROCESSING và submit job HLS ngay lập tức,
        // chúng ta sẽ đổi trạng thái thành PENDING để AI Watermark xử lý trước.
        media.setStatus(MediaStatus.PENDING);
        media.setErrorMessage(null);
        media.setPendingDelete(false);
    }

    @Override
    public String buildHlsUrl(Media media) {
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

    /**
     * Generate a CloudFront signed URL for HLS playback.
     * Falls back to unsigned URL if signing is not configured or fails.
     */
    @Override
    public String buildSignedHlsUrl(Media media, LocalDateTime expiresAt) {
        String url = buildHlsUrl(media);
        if (url == null || url.isBlank()) {
            return media.getFileUrl();
        }

        MediaProperties.Aws aws = mediaProperties.getAws();
        String keyPairId = aws.getCloudfrontKeyPairId();

        if (keyPairId == null || keyPairId.isBlank() || cloudFrontKeyFile == null) {
            log.debug("CloudFront signing not configured, returning unsigned URL. mediaId={}", media.getMediaId());
            return url;
        }

        try {
            Instant expiry = expiresAt.toInstant(ZoneOffset.UTC);

            // Use Custom Policy with wildcard to sign entire HLS directory
            // (master playlist + sub-playlists + .ts segments all need valid signature)
            String hlsDirWildcard = url.substring(0, url.lastIndexOf('/') + 1) + "*";
            CustomSignerRequest request = CustomSignerRequest.builder()
                    .resourceUrl(hlsDirWildcard)
                    .keyPairId(keyPairId)
                    .privateKey(cloudFrontKeyFile)
                    .expirationDate(expiry)
                    .build();

            // Signed URL contains wildcard resource — extract query params and apply to master playlist URL
            String signedWildcard = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request).url();
            String queryParams = signedWildcard.substring(signedWildcard.indexOf('?'));
            return url + queryParams;
        } catch (Exception e) {
            log.warn("Failed to sign CloudFront URL, returning unsigned. mediaId={}", media.getMediaId(), e);
            return url;
        }
    }

    /**
     * Sign a single CloudFront URL using Canned Policy (exact resource, not wildcard).
     */
    @Override
    public String signSingleUrl(String url, LocalDateTime expiresAt) {
        if (url == null || url.isBlank()) {
            return url;
        }
        MediaProperties.Aws aws = mediaProperties.getAws();
        String keyPairId = aws.getCloudfrontKeyPairId();
        if (keyPairId == null || keyPairId.isBlank() || cloudFrontKeyFile == null) {
            return url;
        }
        try {
            Instant expiry = expiresAt.toInstant(ZoneOffset.UTC);
            CannedSignerRequest request = CannedSignerRequest.builder()
                    .resourceUrl(url)
                    .keyPairId(keyPairId)
                    .privateKey(cloudFrontKeyFile)
                    .expirationDate(expiry)
                    .build();
            return cloudFrontUtilities.getSignedUrlWithCannedPolicy(request).url();
        } catch (Exception e) {
            log.warn("Failed to sign URL: {}", e.getMessage());
            return url;
        }
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
    public void purgeAllAssets(Media media) {
        String bucket = mediaProperties.getAws().getBucketName();

        // 1) Source object (raw MP4). May be null for image media or legacy rows — skip if absent.
        String sourceKey = media.getProviderPublicId();
        if (sourceKey != null && !sourceKey.isBlank()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(sourceKey)
                        .build());
                log.info("S3 purge: source object deleted. mediaId={} key={}", media.getMediaId(), sourceKey);
            } catch (Exception e) {
                // Best-effort: a missing/failed source delete must not abort the whole purge.
                log.warn("S3 purge: source delete failed (continuing). mediaId={} key={}",
                        media.getMediaId(), sourceKey, e);
            }
        }

        // 2) MediaConvert output tree (HLS master + variant playlists + .ts segments + thumbnails).
        // deleteAsset() only removes the source key — the output prefix has no env segment and must
        // be reconstructed from episodeId + mediaId (see MediaConvertService output URLs).
        if (media.getEpisode() != null) {
            String outputPrefix = String.format("output/videos/%s/%s/",
                    media.getEpisode().getEpisodeId(), media.getMediaId());
            purgePrefix(bucket, outputPrefix, media.getMediaId());
        }
    }

    /**
     * List every object under a prefix and delete them in batches (S3 caps deleteObjects at 1000
     * keys/request). Paginates via continuation token. Best-effort — logs errors, never throws.
     */
    private void purgePrefix(String bucket, String prefix, String mediaId) {
        int totalDeleted = 0;
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder listBuilder = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix);
                if (continuationToken != null) {
                    listBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listBuilder.build());

                List<ObjectIdentifier> toDelete = new ArrayList<>();
                for (S3Object obj : listResponse.contents()) {
                    toDelete.add(ObjectIdentifier.builder().key(obj.key()).build());
                }

                if (!toDelete.isEmpty()) {
                    DeleteObjectsResponse deleteResponse = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(toDelete).quiet(true).build())
                            .build());
                    totalDeleted += toDelete.size();
                    if (deleteResponse.hasErrors() && !deleteResponse.errors().isEmpty()) {
                        deleteResponse.errors().forEach(err ->
                                log.warn("S3 purge: failed to delete key={} code={} msg={}",
                                        err.key(), err.code(), err.message()));
                    }
                }

                continuationToken = Boolean.TRUE.equals(listResponse.isTruncated())
                        ? listResponse.nextContinuationToken()
                        : null;
            } while (continuationToken != null);

            log.info("S3 purge: output prefix cleared. mediaId={} prefix={} deletedCount={}",
                    mediaId, prefix, totalDeleted);
        } catch (Exception e) {
            log.warn("S3 purge: prefix delete failed (continuing). mediaId={} prefix={} deletedSoFar={}",
                    mediaId, prefix, totalDeleted, e);
        }
    }

    @Override
    public String createHlsPackaging(Media media) {
        try {
            String jobId = mediaConvertService.submitHlsJob(media);
            media.setStatus(MediaStatus.HLS_PROCESSING);
            media.setProviderAssetId(jobId);
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

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
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
