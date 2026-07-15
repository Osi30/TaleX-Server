package com.talex.server.services.media.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.entities.media.Media;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryHlsReconcileService {
    private static final String RECONCILE_ACTOR = "cloudinary-reconcile";
    private static final List<MediaStatus> PROCESSING_STATUSES = List.of(
            MediaStatus.HLS_PROCESSING);

    private final MediaRepository mediaRepository;
    private final MediaProviderService mediaProviderService;
    private final MediaProperties mediaProperties;
    private final AtomicBoolean reconcileActive = new AtomicBoolean(true);
    private final RestClient restClient = RestClient.create();

    @Scheduled(fixedDelayString = "${media.cloudinary.reconcile-fixed-delay-ms:60000}")
    public void reconcileProcessingVideos() {
        MediaProperties.Cloudinary cloudinary = mediaProperties.getCloudinary();
        if (!Boolean.TRUE.equals(cloudinary.getReconcileEnabled())
                || !reconcileActive.get()
                || !hasAdminApiConfig(cloudinary)) {
            return;
        }

        int pageSize = Math.max(1, cloudinary.getReconcilePageSize() == null ? 20 : cloudinary.getReconcilePageSize());
        long staleAfterSeconds = Math.max(0L, cloudinary.getReconcileStaleAfterSeconds() == null
                ? 30L
                : cloudinary.getReconcileStaleAfterSeconds());
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(staleAfterSeconds);

        List<Media> candidates = mediaRepository
                .findAllByProviderAndStatusInAndUpdatedAtBeforeAndProviderPublicIdIsNotNullAndIsDeletedFalseOrderByUpdatedAtAsc(
                        MediaProvider.CLOUDINARY,
                        PROCESSING_STATUSES,
                        staleBefore,
                        PageRequest.of(0, pageSize));

        if (candidates.isEmpty()) {
            deactivateIfNoProcessingMedia();
            return;
        }

        for (Media media : candidates) {
            reconcileMedia(media);
        }
    }

    public void notifyProcessingMedia() {
        reconcileActive.set(true);
    }

    private void deactivateIfNoProcessingMedia() {
        boolean hasProcessingMedia = mediaRepository
                .existsByProviderAndStatusInAndProviderPublicIdIsNotNullAndIsDeletedFalse(
                        MediaProvider.CLOUDINARY,
                        PROCESSING_STATUSES);
        if (!hasProcessingMedia) {
            reconcileActive.set(false);
            log.debug("Cloudinary HLS reconcile paused because no processing media exists.");
        }
    }

    private void reconcileMedia(Media media) {
        try {
            CloudinaryAssetState state = fetchAssetState(media);
            if (state.failed()) {
                media.setStatus(MediaStatus.FAILED);
                media.setErrorMessage(state.errorMessage());
                media.markUpdatedBy(RECONCILE_ACTOR);
                mediaRepository.save(media);
                log.warn("HLS_FAILED reconciled mediaId={} publicId={} error={}",
                        media.getMediaId(), media.getProviderPublicId(), state.errorMessage());
                return;
            }

            if (!state.ready()) {
                requestEagerWebhookRetryIfDue(media);
                return;
            }

            String hlsUrl = firstNonBlank(state.hlsUrl(), media.getHlsUrl(), mediaProviderService.buildHlsUrl(media));
            media.setHlsUrl(hlsUrl);
            media.setPlaybackUrl(hlsUrl);
            if (!StringUtils.hasText(media.getThumbnailUrl())) {
                media.setThumbnailUrl(mediaProviderService.buildThumbnailUrl(media));
            }
            media.setStatus(MediaStatus.HLS_READY);
            media.setErrorMessage(null);
            media.markUpdatedBy(RECONCILE_ACTOR);
            mediaRepository.save(media);

            log.info("HLS_READY reconciled mediaId={} publicId={}", media.getMediaId(), media.getProviderPublicId());
        } catch (Exception ex) {
            log.warn("Cloudinary HLS reconcile skipped. mediaId={} publicId={} error={}",
                    media.getMediaId(), media.getProviderPublicId(), ex.getMessage());
        }
    }

    private void requestEagerWebhookRetryIfDue(Media media) {
        MediaProperties.Cloudinary cloudinary = mediaProperties.getCloudinary();
        if (!StringUtils.hasText(cloudinary.getWebhookUrl())) {
            return;
        }
        if (!shouldRetryEagerWebhook(media, cloudinary)) {
            return;
        }

        String deliveryType = firstNonBlank(media.getProviderDeliveryType(), cloudinary.getProviderDeliveryType(), "upload")
                .toLowerCase(Locale.ROOT);
        long timestamp = System.currentTimeMillis() / 1000L;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("public_id", media.getProviderPublicId());
        params.put("timestamp", String.valueOf(timestamp));
        params.put("type", deliveryType);
        params.put("eager", buildHlsTransformation(cloudinary));
        params.put("eager_async", "true");
        params.put("eager_notification_url", cloudinary.getWebhookUrl());
        params.put("notification_url", cloudinary.getWebhookUrl());
        params.put("overwrite", "true");
        params.put("signature", signApiParams(params, cloudinary.getApiSecret()));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        form.add("api_key", cloudinary.getApiKey());

        String explicitUrl = "%s/v1_1/%s/video/explicit".formatted(
                trimTrailingSlash(cloudinary.getApiBaseUrl()),
                cloudinary.getCloudName());

        restClient.post()
                .uri(URI.create(explicitUrl))
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();

        media.markUpdatedBy(RECONCILE_ACTOR);
        mediaRepository.save(media);
        log.info("Cloudinary eager webhook retry requested. mediaId={} publicId={} webhookUrl={}",
                media.getMediaId(), media.getProviderPublicId(), cloudinary.getWebhookUrl());
    }

    private boolean shouldRetryEagerWebhook(Media media, MediaProperties.Cloudinary cloudinary) {
        long retryAfterSeconds = Math.max(0L, cloudinary.getReconcileEagerRetryAfterSeconds() == null
                ? 300L
                : cloudinary.getReconcileEagerRetryAfterSeconds());
        if (retryAfterSeconds == 0L || media.getUpdatedAt() == null) {
            return true;
        }
        return media.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(retryAfterSeconds));
    }

    private CloudinaryAssetState fetchAssetState(Media media) {
        MediaProperties.Cloudinary cloudinary = mediaProperties.getCloudinary();
        String deliveryType = firstNonBlank(media.getProviderDeliveryType(), cloudinary.getProviderDeliveryType(), "upload")
                .toLowerCase(Locale.ROOT);
        String publicId = encodePublicId(media.getProviderPublicId());
        String url = "%s/v1_1/%s/resources/video/%s/%s?derived=true".formatted(
                trimTrailingSlash(cloudinary.getApiBaseUrl()),
                cloudinary.getCloudName(),
                deliveryType,
                publicId);

        JsonNode root = restClient.get()
                .uri(URI.create(url))
                .headers(headers -> headers.setBasicAuth(cloudinary.getApiKey(), cloudinary.getApiSecret()))
                .retrieve()
                .body(JsonNode.class);

        if (root == null) {
            return CloudinaryAssetState.processing();
        }

        String errorMessage = firstNonBlank(
                textAt(root, "/error/message"),
                textAt(root, "/error"),
                textAt(root, "/message"));
        if (errorMessage != null) {
            return CloudinaryAssetState.failed(errorMessage);
        }

        String hlsUrl = findFirstHlsUrl(root.path("derived"));
        return hlsUrl == null ? CloudinaryAssetState.processing() : CloudinaryAssetState.ready(hlsUrl);
    }

    private boolean hasAdminApiConfig(MediaProperties.Cloudinary cloudinary) {
        return StringUtils.hasText(cloudinary.getCloudName())
                && StringUtils.hasText(cloudinary.getApiKey())
                && StringUtils.hasText(cloudinary.getApiSecret());
    }

    private String findFirstHlsUrl(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.isTextual() && isHlsUrl(node.asText())) {
            return node.asText();
        }

        JsonNode secureUrl = node.get("secure_url");
        if (secureUrl != null && secureUrl.isTextual() && isHlsUrl(secureUrl.asText())) {
            return secureUrl.asText();
        }

        JsonNode url = node.get("url");
        if (url != null && url.isTextual() && isHlsUrl(url.asText())) {
            return url.asText();
        }

        for (JsonNode child : node) {
            String hlsUrl = findFirstHlsUrl(child);
            if (hlsUrl != null) {
                return hlsUrl;
            }
        }
        return null;
    }

    private boolean isHlsUrl(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(".m3u8");
    }

    private String textAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        return node.isMissingNode() || node.isNull() ? null : blankToNull(node.asText());
    }

    private String encodePublicId(String publicId) {
        return URLEncoder.encode(publicId, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String buildHlsTransformation(MediaProperties.Cloudinary cloudinary) {
        String profile = trimSlashes(cloudinary.getHlsStreamingProfile());
        if (profile.isBlank()) {
            profile = "sp_hd";
        }

        if (profile.toLowerCase(Locale.ROOT).startsWith("sp_auto")) {
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

    private String signApiParams(Map<String, String> params, String apiSecret) {
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
        return sha1(payload + apiSecret);
    }

    private String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot generate Cloudinary signature", ex);
        }
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CloudinaryAssetState(boolean ready, boolean failed, String hlsUrl, String errorMessage) {
        private static CloudinaryAssetState ready(String hlsUrl) {
            return new CloudinaryAssetState(true, false, hlsUrl, null);
        }

        private static CloudinaryAssetState processing() {
            return new CloudinaryAssetState(false, false, null, null);
        }

        private static CloudinaryAssetState failed(String errorMessage) {
            return new CloudinaryAssetState(false, true, null, errorMessage);
        }
    }
}
