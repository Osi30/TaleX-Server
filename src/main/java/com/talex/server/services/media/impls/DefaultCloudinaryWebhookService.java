package com.talex.server.services.media.impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.responses.media.CloudinaryWebhookResponseDto;
import com.talex.server.entities.media.Media;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.CloudinaryWebhookService;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCloudinaryWebhookService implements CloudinaryWebhookService {
    private static final Duration SIGNATURE_TOLERANCE = Duration.ofHours(2);
    private static final String WEBHOOK_ACTOR = "cloudinary-webhook";

    private final ObjectMapper objectMapper;
    private final MediaRepository mediaRepository;
    private final MediaProviderService mediaProviderService;
    private final MediaProperties mediaProperties;

    @Transactional
    @Override
    public CloudinaryWebhookResponseDto handleNotification(String payload, String signature, String timestamp) {
        validateSignature(payload, signature, timestamp);

        JsonNode root = parsePayload(payload);
        String notificationType = lower(firstNonBlank(
                textAt(root, "/notification_type"),
                textAt(root, "/event/type"),
                textAt(root, "/raw/notification_type")));
        String publicId = firstNonBlank(
                textAt(root, "/public_id"),
                textAt(root, "/asset/public_id"),
                textAt(root, "/raw/public_id"),
                findFirstTextByName(root, "public_id"));

        log.info("WEBHOOK_RECEIVED provider=cloudinary type={} publicId={} status={} eager={}",
                notificationType,
                publicId,
                firstNonBlank(textAt(root, "/status"), textAt(root, "/eager/status")),
                summarizeEager(root.path("eager")));

        if (publicId == null) {
            log.warn("WEBHOOK_RECEIVED ignored because public_id is missing. type={}", notificationType);
            return response("ignored", "missing_public_id", null, null);
        }

        Media media = mediaRepository.findFirstByProviderPublicIdAndIsDeletedFalse(publicId)
                .orElse(null);
        if (media == null) {
            log.warn("WEBHOOK_RECEIVED ignored because media was not found. publicId={}", publicId);
            return response("ignored", "media_not_found", publicId, null);
        }

        if (isFailure(root, notificationType)) {
            return markFailed(media, root, notificationType);
        }

        applyUploadMetadata(media, root, publicId);

        if (isEagerReady(root, notificationType)) {
            return markHlsReady(media, root, publicId);
        }

        if ("upload".equals(notificationType)) {
            return markHlsProcessing(media, publicId);
        }

        mediaRepository.save(media);
        return response("ignored", firstNonBlank(notificationType, "unhandled"), publicId, media.getMediaId());
    }

    private void validateSignature(String payload, String signature, String timestamp) {
        String secret = firstNonBlank(
                mediaProperties.getCloudinary().getWebhookSigningSecret(),
                mediaProperties.getCloudinary().getApiSecret());
        if (secret == null) {
            log.warn("WEBHOOK_RECEIVED rejected because Cloudinary webhook signing secret is not configured.");
            throw ContentModuleException.unauthorized("Cloudinary webhook signing secret is not configured");
        }

        if (signature == null || signature.isBlank() || timestamp == null || timestamp.isBlank()) {
            throw ContentModuleException.unauthorized("Invalid Cloudinary webhook signature");
        }

        if (!isFreshTimestamp(timestamp.trim())) {
            throw ContentModuleException.unauthorized("Expired Cloudinary webhook signature");
        }

        String signedPayload = payload + timestamp.trim() + secret;
        if (!secureEquals(signature.trim(), digest("SHA-1", signedPayload))
                && !secureEquals(signature.trim(), digest("SHA-256", signedPayload))) {
            throw ContentModuleException.unauthorized("Invalid Cloudinary webhook signature");
        }
    }

    private boolean isFreshTimestamp(String timestamp) {
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Duration age = Duration.between(signedAt, Instant.now()).abs();
            return age.compareTo(SIGNATURE_TOLERANCE) <= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String digest(String algorithm, String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm)
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw ContentModuleException.badRequest("Cannot validate Cloudinary webhook signature");
        }
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw ContentModuleException.badRequest("Malformed Cloudinary webhook payload");
        }
    }

    private CloudinaryWebhookResponseDto markHlsReady(Media media, JsonNode root, String publicId) {
        String hlsUrl = firstNonBlank(extractHlsUrl(root), media.getHlsUrl(), mediaProviderService.buildHlsUrl(media));
        media.setHlsUrl(hlsUrl);
        media.setPlaybackUrl(hlsUrl);
        if (media.getThumbnailUrl() == null || media.getThumbnailUrl().isBlank()) {
            media.setThumbnailUrl(mediaProviderService.buildThumbnailUrl(media));
        }
        media.setStatus(MediaStatus.HLS_READY);
        media.setErrorMessage(null);
        media.markUpdatedBy(WEBHOOK_ACTOR);
        media = mediaRepository.save(media);

        log.info("HLS_READY mediaId={} providerPublicId={}", media.getMediaId(), publicId);
        return response("processed", "hls_ready", publicId, media.getMediaId());
    }

    private CloudinaryWebhookResponseDto markHlsProcessing(Media media, String publicId) {
        if (media.getStatus() == MediaStatus.PROCESSING || media.getStatus() == MediaStatus.HLS_PROCESSING) {
            media.setStatus(MediaStatus.HLS_PROCESSING);
            media.markUpdatedBy(WEBHOOK_ACTOR);
            media = mediaRepository.save(media);
            log.info("HLS_PROCESSING_STARTED mediaId={} providerPublicId={}", media.getMediaId(), publicId);
        }
        return response("processed", "hls_processing", publicId, media.getMediaId());
    }

    private CloudinaryWebhookResponseDto markFailed(Media media, JsonNode root, String notificationType) {
        String message = firstNonBlank(
                textAt(root, "/error/message"),
                textAt(root, "/reason"),
                findFirstTextByName(root, "reason"),
                textAt(root, "/message"),
                findFirstTextByName(root, "message"),
                "Cloudinary video processing failed");
        media.setStatus(MediaStatus.FAILED);
        media.setErrorMessage(message);
        media.markUpdatedBy(WEBHOOK_ACTOR);
        media = mediaRepository.save(media);

        log.warn("HLS_FAILED mediaId={} providerPublicId={} type={} error={}",
                media.getMediaId(), media.getProviderPublicId(), notificationType, message);
        return response("processed", "hls_failed", media.getProviderPublicId(), media.getMediaId());
    }

    private void applyUploadMetadata(Media media, JsonNode root, String publicId) {
        media.setProviderPublicId(publicId);
        setIfBlank(media::setProviderAssetId, media.getProviderAssetId(), textAt(root, "/asset_id"));
        String secureUrl = firstNonBlank(textAt(root, "/secure_url"), textAt(root, "/asset/secure_url"));
        if (secureUrl != null) {
            if (media.getOriginalUrl() == null || media.getOriginalUrl().isBlank()) {
                media.setOriginalUrl(secureUrl);
            }
            if (media.getFileUrl() == null || media.getFileUrl().isBlank() || media.getFileUrl().startsWith("pending://")) {
                media.setFileUrl(secureUrl);
            }
        }

        Long bytes = longAt(root, "/bytes");
        if (bytes != null && bytes >= 0) {
            media.setFileSize(bytes);
        }
        Integer width = intAt(root, "/width");
        Integer height = intAt(root, "/height");
        if (width != null) {
            media.setWidth(width);
        }
        if (height != null) {
            media.setHeight(height);
        }
        if (width != null && height != null) {
            media.setResolution(width + "x" + height);
        }
        Long duration = longAt(root, "/duration");
        if (duration != null) {
            media.setDuration(duration);
        }
        String format = textAt(root, "/format");
        if (format != null) {
            media.setFormat(format);
        }
        media.markUpdatedBy(WEBHOOK_ACTOR);
    }

    private boolean isFailure(JsonNode root, String notificationType) {
        return "error".equals(notificationType)
                || "error".equals(lower(textAt(root, "/status")))
                || "failed".equals(lower(textAt(root, "/status")))
                || "error".equals(lower(textAt(root, "/eager/status")))
                || "failed".equals(lower(textAt(root, "/eager/status")))
                || hasMeaningfulError(root.path("error"))
                || hasFailedEagerItem(root.path("eager"));
    }

    private boolean hasFailedEagerItem(JsonNode eagerNode) {
        if (!eagerNode.isArray()) {
            return false;
        }
        for (JsonNode item : eagerNode) {
            String status = lower(textAt(item, "/status"));
            if ("failed".equals(status) || "error".equals(status) || hasMeaningfulError(item.path("error"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMeaningfulError(JsonNode errorNode) {
        if (errorNode == null || errorNode.isMissingNode() || errorNode.isNull()) {
            return false;
        }

        if (errorNode.isTextual()) {
            return blankToNull(errorNode.asText()) != null;
        }

        if (errorNode.isObject()) {
            String message = firstNonBlank(
                    textAt(errorNode, "/message"),
                    textAt(errorNode, "/detail"),
                    textAt(errorNode, "/reason"));
            return message != null;
        }

        return !errorNode.isEmpty();
    }

    private boolean isEagerReady(JsonNode root, String notificationType) {
        return "eager".equals(notificationType)
                || extractHlsUrl(root) != null
                || hasReadyEagerItem(root.path("eager"));
    }

    private boolean hasReadyEagerItem(JsonNode eagerNode) {
        if (!eagerNode.isArray()) {
            return false;
        }
        for (JsonNode item : eagerNode) {
            String status = lower(textAt(item, "/status"));
            if ("complete".equals(status) || "completed".equals(status) || "success".equals(status) || "ok".equals(status)) {
                return true;
            }
        }
        return false;
    }

    private String extractHlsUrl(JsonNode root) {
        String direct = firstNonBlank(
                textAt(root, "/hls_url"),
                textAt(root, "/playback_url"),
                textAt(root, "/manifest_url"));
        if (isHlsUrl(direct)) {
            return direct;
        }

        String secureUrl = findFirstHlsUrlByName(root, "secure_url");
        if (secureUrl != null) {
            return secureUrl;
        }
        return findFirstHlsUrlByName(root, "url");
    }

    private String summarizeEager(JsonNode eagerNode) {
        if (!eagerNode.isArray() || eagerNode.isEmpty()) {
            return null;
        }

        StringBuilder summary = new StringBuilder();
        for (JsonNode item : eagerNode) {
            if (!summary.isEmpty()) {
                summary.append(" | ");
            }
            summary.append(firstNonBlank(textAt(item, "/transformation"), "unknown"))
                    .append(":")
                    .append(firstNonBlank(textAt(item, "/status"), "unknown"));
            String reason = textAt(item, "/reason");
            if (reason != null) {
                summary.append("(").append(reason).append(")");
            }
        }
        return summary.toString();
    }

    private String findFirstHlsUrlByName(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode direct = node.get(fieldName);
        if (direct != null && direct.isTextual() && isHlsUrl(direct.asText())) {
            return direct.asText();
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            String value = findFirstHlsUrlByName(children.next(), fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String findFirstTextByName(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode direct = node.get(fieldName);
        if (direct != null && direct.isValueNode()) {
            return blankToNull(direct.asText());
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            String value = findFirstTextByName(children.next(), fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String textAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        return node.isMissingNode() || node.isNull() ? null : blankToNull(node.asText());
    }

    private Integer intAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (!node.isNumber()) {
            return null;
        }
        return node.asInt();
    }

    private Long longAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (!node.isNumber()) {
            return null;
        }
        return node.isFloatingPointNumber() ? Math.round(node.asDouble()) : node.asLong();
    }

    private boolean isHlsUrl(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(".m3u8");
    }

    private void setIfBlank(java.util.function.Consumer<String> setter, String currentValue, String nextValue) {
        if ((currentValue == null || currentValue.isBlank()) && nextValue != null) {
            setter.accept(nextValue);
        }
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
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

    private CloudinaryWebhookResponseDto response(String status, String action, String publicId, String mediaId) {
        return CloudinaryWebhookResponseDto.builder()
                .status(status)
                .action(action)
                .publicId(publicId)
                .mediaId(mediaId)
                .build();
    }
}
