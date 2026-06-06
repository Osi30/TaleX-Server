package com.talex.server.configs.properties;

import com.talex.server.enums.MediaProtectionType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "media")
@Data
public class MediaProperties {
    private String appEnv = "local";
    private Long maxVideoSizeMb = 2048L;
    private Long videoUploadChunkSizeMb = 10L;
    private Long minVideoUploadChunkSizeMb = 5L;
    private List<String> allowedVideoMimeTypes = new ArrayList<>(
            List.of("video/mp4", "video/quicktime", "video/webm", "video/x-matroska"));
    private MediaProtectionType defaultVideoProtectionType = MediaProtectionType.SIGNED_URL;
    private String playbackSigningSecret = "";
    private Long signedPlaybackTtlSeconds = 300L;
    private Boolean enableDrm = false;
    private String drmProvider = "NONE";
    private String drmLicenseServerUrl = "";
    private String drmCertificateUrl = "";
    private Cloudinary cloudinary = new Cloudinary();

    @Data
    public static class Cloudinary {
        private String cloudName = "";
        private String apiKey = "";
        private String apiSecret = "";
        private String videoFolder = "talex";
        private String imageFolder = "talex";
        private String hlsStreamingProfile = "sp_hd";
        private String providerDeliveryType = "authenticated";
        private String authTokenKey = "";
        private String webhookUrl = "";
        private String webhookSigningSecret = "";
        private String apiBaseUrl = "https://api.cloudinary.com";
        private String deliveryBaseUrl = "https://res.cloudinary.com";
    }
}
