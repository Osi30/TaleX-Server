package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.MediaSystemConfigRequestDto;
import com.talex.server.dtos.responses.media.MediaSystemConfigResponseDto;

public interface MediaSystemConfigService {
    MediaSystemConfigResponseDto getConfig();
    MediaSystemConfigResponseDto updateConfig(MediaSystemConfigRequestDto request);
}
