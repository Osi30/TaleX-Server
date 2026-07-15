package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.media.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.media.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.media.VideoUploadSessionRequestDto;
import com.talex.server.dtos.responses.media.MediaResponseDto;
import com.talex.server.dtos.responses.media.MediaUploadSessionResponseDto;
import com.talex.server.dtos.responses.media.VideoUploadSessionResponseDto;

public interface MediaUploadSessionService {
    VideoUploadSessionResponseDto createVideoUploadSession(String episodeId, VideoUploadSessionRequestDto request);

    MediaUploadSessionResponseDto getSession(String uploadSessionId);

    MediaUploadSessionResponseDto updateProgress(String uploadSessionId, MediaUploadProgressRequestDto request);

    MediaUploadSessionResponseDto pause(String uploadSessionId, String actorId);

    MediaUploadSessionResponseDto fail(String uploadSessionId, MediaUploadFailRequestDto request);

    MediaUploadSessionResponseDto cancel(String uploadSessionId, String actorId);

    MediaResponseDto complete(String uploadSessionId, MediaUploadCompleteRequestDto request);
}
