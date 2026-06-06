package com.talex.server.services;

import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.VideoUploadSessionRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.dtos.responses.MediaUploadSessionResponseDto;
import com.talex.server.dtos.responses.VideoUploadSessionResponseDto;

public interface MediaUploadSessionService {
    VideoUploadSessionResponseDto createVideoUploadSession(String episodeId, VideoUploadSessionRequestDto request);

    MediaUploadSessionResponseDto getSession(String uploadSessionId);

    MediaUploadSessionResponseDto updateProgress(String uploadSessionId, MediaUploadProgressRequestDto request);

    MediaUploadSessionResponseDto pause(String uploadSessionId, String actorId);

    MediaUploadSessionResponseDto fail(String uploadSessionId, MediaUploadFailRequestDto request);

    MediaUploadSessionResponseDto cancel(String uploadSessionId, String actorId);

    MediaResponseDto complete(String uploadSessionId, MediaUploadCompleteRequestDto request);
}
