package com.talex.server.dtos.responses.series;

import com.talex.server.dtos.responses.media.DrmPlaybackConfigDto;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaType;
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
    private Boolean isLocked;
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
