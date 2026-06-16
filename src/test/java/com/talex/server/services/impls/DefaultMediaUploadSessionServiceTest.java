package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.entities.Episode;
import com.talex.server.entities.Media;
import com.talex.server.entities.MediaUploadSession;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.enums.MediaUploadSessionStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.MediaUploadSessionRepository;
import com.talex.server.services.MediaService;
import com.talex.server.services.media.MediaProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMediaUploadSessionServiceTest {
    @Mock
    private MediaUploadSessionRepository uploadSessionRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private MediaProviderService mediaProviderService;

    @Mock
    private CloudinaryHlsReconcileService cloudinaryHlsReconcileService;

    @Mock
    private MediaUploadProgressCache uploadProgressCache;

    private DefaultMediaUploadSessionService service;

    @BeforeEach
    void setUp() {
        lenient().when(uploadProgressCache.get(any())).thenReturn(Optional.empty());
        service = new DefaultMediaUploadSessionService(
                uploadSessionRepository,
                mediaRepository,
                episodeRepository,
                mediaService,
                mediaProviderService,
                new MediaProperties(),
                cloudinaryHlsReconcileService,
                uploadProgressCache);
    }

    @Test
    void updateProgressSkipsSaveWhenProgressIsUnchanged() {
        MediaUploadSession session = session(MediaUploadSessionStatus.UPLOADING, 1024L, 0);
        session.markUpdatedBy("actor-1");
        when(uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse("session-1"))
                .thenReturn(Optional.of(session));

        var response = service.updateProgress(
                "session-1",
                new MediaUploadProgressRequestDto(1024L, 0, MediaUploadSessionStatus.UPLOADING, "actor-1"));

        assertEquals(1024L, response.getUploadedBytes());
        verify(uploadSessionRepository, never()).save(any(MediaUploadSession.class));
    }

    @Test
    void updateProgressIgnoresStaleProgressWithoutRegressingSession() {
        MediaUploadSession session = session(MediaUploadSessionStatus.UPLOADING, 2048L, 1);
        when(uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse("session-1"))
                .thenReturn(Optional.of(session));

        var response = service.updateProgress(
                "session-1",
                new MediaUploadProgressRequestDto(1024L, 0, MediaUploadSessionStatus.UPLOADING, "actor-1"));

        assertEquals(2048L, response.getUploadedBytes());
        assertEquals(2048L, session.getUploadedBytes());
        verify(uploadSessionRepository, never()).save(any(MediaUploadSession.class));
    }

    @Test
    void updateProgressStoresProgressInRedisWithoutSavingDatabase() {
        MediaUploadSession session = session(MediaUploadSessionStatus.UPLOADING, 1024L, 0);
        when(uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse("session-1"))
                .thenReturn(Optional.of(session));
        when(uploadProgressCache.put(eq("session-1"), any(CachedMediaUploadProgress.class), any(MediaUploadSession.class)))
                .thenReturn(true);

        var response = service.updateProgress(
                "session-1",
                new MediaUploadProgressRequestDto(2048L, 1, MediaUploadSessionStatus.UPLOADING, "actor-1"));

        assertEquals(2048L, response.getUploadedBytes());
        assertEquals(1, response.getLastUploadedChunkIndex());
        verify(uploadProgressCache).put(eq("session-1"), any(CachedMediaUploadProgress.class), any(MediaUploadSession.class));
        verify(uploadSessionRepository, never()).save(any(MediaUploadSession.class));
    }

    @Test
    void updateProgressRejectsNonProgressStatus() {
        MediaUploadSession session = session(MediaUploadSessionStatus.UPLOADING, 1024L, 0);
        when(uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse("session-1"))
                .thenReturn(Optional.of(session));

        assertThrows(ContentModuleException.class, () -> service.updateProgress(
                "session-1",
                new MediaUploadProgressRequestDto(2048L, 1, MediaUploadSessionStatus.COMPLETED, "actor-1")));

        verify(uploadSessionRepository, never()).save(any(MediaUploadSession.class));
    }

    @Test
    void completeIsIdempotentAfterSessionAlreadyCompleted() {
        MediaUploadSession session = session(MediaUploadSessionStatus.COMPLETED, 2048L, 1);
        Media media = session.getMedia();
        media.setStatus(MediaStatus.HLS_PROCESSING);
        when(uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse("session-1"))
                .thenReturn(Optional.of(session));
        when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse("episode-1"))
                .thenReturn(Optional.of(session.getEpisode()));
        when(mediaService.toResponse(media))
                .thenReturn(MediaResponseDto.builder().mediaId("media-1").build());

        var response = service.complete(
                "session-1",
                new MediaUploadCompleteRequestDto(
                        "asset-1",
                        "talex/local/videos/episode-1/media-1",
                        "https://res.cloudinary.com/demo/video/authenticated/talex/local/videos/episode-1/media-1.mp4",
                        "video",
                        "mp4",
                        2048L,
                        12.0,
                        1920,
                        1080,
                        "actor-1"));

        assertEquals("media-1", response.getMediaId());
        verify(mediaProviderService, never()).applyCompletedUpload(any(), any(), any());
        verify(mediaRepository, never()).save(any(Media.class));
        verify(uploadSessionRepository, never()).save(any(MediaUploadSession.class));
    }

    private MediaUploadSession session(
            MediaUploadSessionStatus status,
            Long uploadedBytes,
            Integer lastUploadedChunkIndex) {
        Episode episode = new Episode();
        episode.setEpisodeId("episode-1");

        Media media = new Media();
        media.setMediaId("media-1");
        media.setEpisode(episode);
        media.setMediaType(MediaType.VIDEO);
        media.setMimeType("video/mp4");
        media.setFileSize(2048L);
        media.setProvider(MediaProvider.CLOUDINARY);
        media.setProviderPublicId("talex/local/videos/episode-1/media-1");

        MediaUploadSession session = new MediaUploadSession();
        session.setUploadSessionId("session-1");
        session.setMedia(media);
        session.setEpisode(episode);
        session.setProvider(MediaProvider.CLOUDINARY);
        session.setProviderPublicId("talex/local/videos/episode-1/media-1");
        session.setProviderDeliveryType("authenticated");
        session.setUploadUniqueId("unique-1");
        session.setFileName("video.mp4");
        session.setFileSize(2048L);
        session.setMimeType("video/mp4");
        session.setChunkSize(1024L);
        session.setTotalChunks(2);
        session.setUploadedBytes(uploadedBytes);
        session.setLastUploadedChunkIndex(lastUploadedChunkIndex);
        session.setStatus(status);
        return session;
    }
}
