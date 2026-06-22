package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.entities.Media;
import com.talex.server.entities.MediaUploadSession;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.SignedUploadParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryMediaProviderService implements MediaProviderService, MediaPackagingService {
    private static final String RESOURCE_TYPE_VIDEO = "video";

    private final MediaProperties mediaProperties;
    private final AtomicBoolean spAutoWarningLogged = new AtomicBoolean(false);

    @Override
    public com.talex.server.dtos.responses.ImagePresignedUploadResponseDto createImagePresignedUpload(
            com.talex.server.dtos.requests.ImagePresignedUploadRequestDto request) {
        // Cloudinary images are uploaded directly from frontend — no presigned URL needed
        throw new UnsupportedOperationException("Image presigned upload not supported for Cloudinary provider");
    }

    @Override
    public SignedUploadParams createSignedUploadParams(String providerPublicId, String providerDeliveryType) {
        ensureConfigured();

        long timestamp = System.currentTimeMillis() / 1000L;
        Map<String, String> uploadParams = new LinkedHashMap<>();
        uploadParams.put("public_id", providerPublicId);
        uploadParams.put("timestamp", String.valueOf(timestamp));
        uploadParams.put("type", providerDeliveryType);
        uploadParams.put("eager", buildHlsTransformation());
        uploadParams.put("eager_async", "true");
        uploadParams.put("overwrite", "true");
        if (!mediaProperties.getCloudinary().getWebhookUrl().isBlank()) {
            uploadParams.put("eager_notification_url", mediaProperties.getCloudinary().getWebhookUrl());
        }

        String signature = signApiParams(uploadParams);
        String uploadUrl = "%s/v1_1/%s/%s/upload".formatted(
                mediaProperties.getCloudinary().getApiBaseUrl(),
                mediaProperties.getCloudinary().getCloudName(),
                RESOURCE_TYPE_VIDEO);

        log.info("Cloudinary signed video upload params created. publicId={} deliveryType={} eager={} webhookConfigured={}",
                providerPublicId,
                providerDeliveryType,
                uploadParams.get("eager"),
                uploadParams.containsKey("eager_notification_url"));

        return SignedUploadParams.builder()
                .cloudName(mediaProperties.getCloudinary().getCloudName())
                .apiKey(mediaProperties.getCloudinary().getApiKey())
                .timestamp(timestamp)
                .signature(signature)
                .uploadUrl(uploadUrl)
                .resourceType(RESOURCE_TYPE_VIDEO)
                .uploadParams(uploadParams)
                .build();
    }

    @Override
    public String buildVideoPublicId(String episodeId, String mediaId) {
        String baseFolder = trimSlashes(mediaProperties.getCloudinary().getVideoFolder());
        String env = sanitizePathPart(mediaProperties.getAppEnv());
        return "%s/%s/videos/%s/%s".formatted(
                baseFolder.isBlank() ? "talex" : baseFolder,
                env.isBlank() ? "local" : env,
                sanitizePathPart(episodeId),
                sanitizePathPart(mediaId));
    }

    @Override
    public void applyCompletedUpload(Media media, MediaUploadSession session, MediaUploadCompleteRequestDto request) {
        media.setProvider(MediaProvider.CLOUDINARY);
        media.setProviderAssetId(blankToNull(request.getAssetId()));
        media.setProviderPublicId(request.getPublicId());
        media.setProviderDeliveryType(session.getProviderDeliveryType());
        media.setOriginalUrl(request.getSecureUrl());
        media.setFileUrl(request.getSecureUrl());
        media.setPlaybackUrl(buildHlsUrl(media));
        media.setHlsUrl(buildHlsUrl(media));
        media.setThumbnailUrl(buildThumbnailUrl(media));
        media.setFormat(blankToNull(request.getFormat()));
        media.setMimeType(session.getMimeType());
        media.setFileSize(request.getBytes());
        media.setWidth(request.getWidth());
        media.setHeight(request.getHeight());
        media.setDuration(request.getDuration() == null ? null : Math.round(request.getDuration()));
        media.setResolution(request.getWidth() != null && request.getHeight() != null
                ? request.getWidth() + "x" + request.getHeight()
                : null);
        media.setChecksum(sha256(request.getPublicId() + ":" + request.getBytes()));
        media.setMediaType(MediaType.VIDEO);
        media.setStatus(MediaStatus.HLS_PROCESSING);
        media.setErrorMessage(null);
        media.setPendingDelete(false);
    }

    @Override
    public String buildHlsUrl(Media media) {
        String deliveryType = normalizeDeliveryType(media.getProviderDeliveryType());
        return "%s/%s/%s/%s/%s.m3u8".formatted(
                trimTrailingSlash(mediaProperties.getCloudinary().getDeliveryBaseUrl()),
                mediaProperties.getCloudinary().getCloudName(),
                RESOURCE_TYPE_VIDEO,
                deliveryType,
                buildHlsTransformation() + "/" + media.getProviderPublicId());
    }

    @Override
    public String buildSignedHlsUrl(Media media, LocalDateTime expiresAt) {
        String deliveryType = normalizeDeliveryType(media.getProviderDeliveryType());
        String transformationAndAsset = buildHlsTransformation()
                + "/" + media.getProviderPublicId() + ".m3u8";
        String signature = signDeliveryPath(transformationAndAsset);
        String url = "%s/%s/%s/%s/s--%s--/%s".formatted(
                trimTrailingSlash(mediaProperties.getCloudinary().getDeliveryBaseUrl()),
                mediaProperties.getCloudinary().getCloudName(),
                RESOURCE_TYPE_VIDEO,
                deliveryType,
                signature,
                transformationAndAsset);

        String authTokenKey = mediaProperties.getCloudinary().getAuthTokenKey();
        if (authTokenKey != null && !authTokenKey.isBlank()) {
            long expiration = expiresAt.toEpochSecond(ZoneOffset.UTC);
            String tokenPayload = "exp=" + expiration + "~acl=/" + RESOURCE_TYPE_VIDEO + "/" + deliveryType + "/*";
            String hmac = sha256HmacHex(tokenPayload, authTokenKey);
            return url + "?__cld_token__=" + tokenPayload + "~hmac=" + hmac;
        }

        log.warn("Cloudinary auth token key is not configured. Signed delivery URL reduces tampering but does not add TTL enforcement at CDN level.");
        return url;
    }

    @Override
    public String buildThumbnailUrl(Media media) {
        String deliveryType = normalizeDeliveryType(media.getProviderDeliveryType());
        String transformation = "so_0,w_640,c_limit,f_jpg";
        String path = transformation + "/" + media.getProviderPublicId() + ".jpg";
        String signature = signDeliveryPath(path);
        return "%s/%s/%s/%s/s--%s--/%s".formatted(
                trimTrailingSlash(mediaProperties.getCloudinary().getDeliveryBaseUrl()),
                mediaProperties.getCloudinary().getCloudName(),
                RESOURCE_TYPE_VIDEO,
                deliveryType,
                signature,
                path);
    }

    @Override
    public void deleteAsset(Media media) {
        if (media.getProviderPublicId() == null || media.getProviderPublicId().isBlank()) {
            return;
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("public_id", media.getProviderPublicId());
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
            params.put("type", normalizeDeliveryType(media.getProviderDeliveryType()));
            params.put("signature", signApiParams(params));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("public_id", params.get("public_id"));
            form.add("timestamp", params.get("timestamp"));
            form.add("type", params.get("type"));
            form.add("signature", params.get("signature"));
            form.add("api_key", mediaProperties.getCloudinary().getApiKey());

            String destroyUrl = "%s/v1_1/%s/%s/destroy".formatted(
                    mediaProperties.getCloudinary().getApiBaseUrl(),
                    mediaProperties.getCloudinary().getCloudName(),
                    RESOURCE_TYPE_VIDEO);

            RestClient.create()
                    .post()
                    .uri(destroyUrl)
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Cloudinary video deleted. mediaId={} publicId={}", media.getMediaId(), media.getProviderPublicId());
        } catch (Exception ex) {
            media.setPendingDelete(true);
            media.setErrorMessage("Cloudinary delete failed: " + ex.getMessage());
            log.warn("Cloudinary video delete failed. mediaId={} publicId={}", media.getMediaId(), media.getProviderPublicId(), ex);
        }
    }

    @Override
    public String createHlsPackaging(Media media) {
        return buildHlsUrl(media);
    }

    @Override
    public String createDashPackaging(Media media) {
        return null;
    }

    @Override
    public String getManifestUrl(Media media) {
        return buildHlsUrl(media);
    }

    private void ensureConfigured() {
        if (mediaProperties.getCloudinary().getCloudName().isBlank()
                || mediaProperties.getCloudinary().getApiKey().isBlank()
                || mediaProperties.getCloudinary().getApiSecret().isBlank()) {
            throw ContentModuleException.badRequest("Cloudinary signed upload is not configured");
        }
    }

    private String buildHlsTransformation() {
        String profile = trimSlashes(mediaProperties.getCloudinary().getHlsStreamingProfile());
        if (profile.isBlank()) {
            profile = "sp_hd";
        }

        if (profile.toLowerCase(Locale.ROOT).startsWith("sp_auto")) {
            if (spAutoWarningLogged.compareAndSet(false, true)) {
                log.warn("Cloudinary sp_auto cannot be used for eager HLS webhook processing. Using sp_hd/f_m3u8 instead. Set CLOUDINARY_HLS_STREAMING_PROFILE to sp_hd, sp_full_hd, or sp_sd for explicit control.");
            }
            profile = "sp_hd";
        }

        String normalized = profile
                .replace(",f_m3u8", "/f_m3u8")
                .replace("f_m3u8,", "f_m3u8/")
                .replaceAll("/+", "/");

        if (normalized.contains("f_m3u8")) {
            return normalized;
        }

        return normalized + "/f_m3u8";
    }

    private String signApiParams(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.remove("file");
        sorted.remove("api_key");
        sorted.remove("cloud_name");
        sorted.remove("resource_type");
        sorted.remove("signature");

        String payload = sorted.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return sha1(payload + mediaProperties.getCloudinary().getApiSecret());
    }

    private String signDeliveryPath(String path) {
        String digest = sha1Bytes(path + mediaProperties.getCloudinary().getApiSecret());
        byte[] bytes = HexFormat.of().parseHex(digest);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 8);
    }

    private String sha1(String value) {
        return HexFormat.of().formatHex(sha1Raw(value));
    }

    private String sha1Bytes(String value) {
        return HexFormat.of().formatHex(sha1Raw(value));
    }

    private byte[] sha1Raw(String value) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw ContentModuleException.badRequest("Cannot generate Cloudinary signature");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw ContentModuleException.badRequest("Cannot generate checksum");
        }
    }

    private String sha256HmacHex(String payload, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw ContentModuleException.badRequest("Cannot generate Cloudinary auth token");
        }
    }

    private String normalizeDeliveryType(String deliveryType) {
        if (deliveryType == null || deliveryType.isBlank()) {
            return mediaProperties.getCloudinary().getProviderDeliveryType();
        }
        return deliveryType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizePathPart(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

