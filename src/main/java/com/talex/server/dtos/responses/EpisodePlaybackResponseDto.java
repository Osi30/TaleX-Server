package com.talex.server.dtos.responses;

import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodePlaybackResponseDto {
    private String episodeId;
    private String mediaId;
    private MediaType mediaType;
    private String playbackType;
    private MediaProvider provider;
    private MediaProtectionType protectionType;
    private String hlsUrl;
    private String playbackUrl;
    private String manifestUrl;
    private String thumbnailUrl;
    private Long duration;
    private LocalDateTime expiresAt;
    private DrmPlaybackConfigDto drm;
    private String token;
}
