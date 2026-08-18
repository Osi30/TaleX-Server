package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaCopyright;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.ContentPipelineService;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MediaPurgeServiceImpl}.
 *
 * Tests orchestration logic: guard against source-media references (D2), playback session
 * revocation, hard-delete (via helper), and best-effort external cleanup (S3, Milvus).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaPurgeServiceImpl")
class MediaPurgeServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaCopyrightRepository mediaCopyrightRepository;

    @Mock
    private MediaHardDeleteHelper mediaHardDeleteHelper;

    @Mock
    private MediaPlaybackSecurityService playbackSecurityService;

    @Mock
    private MediaProviderService mediaProviderService;

    @Mock
    private ContentPipelineService contentPipelineService;

    @InjectMocks
    private MediaPurgeServiceImpl mediaPurgeService;

    private String testMediaId;
    private String testAdminId;
    private String testReason;
    private Media testMedia;

    @BeforeEach
    void setUp() {
        testMediaId = "media-001";
        testAdminId = "admin-uuid-001";
        testReason = "DMCA takedown notice #12345";

        testMedia = new Media();
        testMedia.setMediaId(testMediaId);
        testMedia.setFileUrl("s3://talex-media/media-001.mp4");
        testMedia.setProviderPublicId("provider-id-001");
        testMedia.setFileSize(1024000L);
        testMedia.setCreatorId("creator-001");

        Episode episode = new Episode();
        episode.setEpisodeId("episode-001");
        testMedia.setEpisode(episode);
    }

    @Nested
    @DisplayName("Media Not Found (404)")
    class MediaNotFoundTests {

        @Test
        @DisplayName("Should throw exception when media does not exist")
        void mediaNotFound_throwsNotFound() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.empty());

            ContentModuleException exception = assertThrows(ContentModuleException.class,
                    () -> mediaPurgeService.purge(testMediaId, testAdminId, testReason));

            assertTrue(exception.getMessage().contains("not found"));
            verify(mediaRepository).findByMediaId(testMediaId);
            verifyNoMoreInteractions(mediaRepository, mediaHardDeleteHelper, playbackSecurityService);
        }
    }

    @Nested
    @DisplayName("Business Rule D2: Source Media Reference Guard")
    class SourceMediaReferenceGuardTests {

        @Test
        @DisplayName("Should block purge with 409 when media is source of another violation")
        void sourcedByOtherViolation_throwsConflict() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            // This media is referenced as the SOURCE of another media's copyright violation
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(true);

            ContentModuleException exception = assertThrows(ContentModuleException.class,
                    () -> mediaPurgeService.purge(testMediaId, testAdminId, testReason));

            assertTrue(exception.getMessage().contains("Cannot purge")
                    && exception.getMessage().contains("source"));
            // Verify no destructive operations occurred
            verify(playbackSecurityService, never()).revokeActiveSessions(any());
            verify(mediaHardDeleteHelper, never()).hardDelete(any(), anyString(), anyString());
            verify(mediaProviderService, never()).purgeAllAssets(any());
            verify(contentPipelineService, never()).notifyMediaDeleted(anyString());
        }

        @Test
        @DisplayName("Should allow purge when media is not source of any violation")
        void notASourceMedia_proceedsWithPurge() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            // Verify orchestration proceeded normally
            verify(playbackSecurityService).revokeActiveSessions(testMedia);
            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            verify(mediaProviderService).purgeAllAssets(testMedia);
            verify(contentPipelineService).notifyMediaDeleted(testMediaId);
        }
    }

    @Nested
    @DisplayName("Orchestration Order and Side-Effects")
    class OrchestrationTests {

        @Test
        @DisplayName("Should revoke playback sessions before hard-delete")
        void orchestration_revokeBeforeDelete() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            InOrder inOrder = inOrder(playbackSecurityService, mediaHardDeleteHelper);
            inOrder.verify(playbackSecurityService).revokeActiveSessions(testMedia);
            inOrder.verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
        }

        @Test
        @DisplayName("Should run S3 purge after DB delete (best-effort)")
        void orchestration_s3PurgeAfterDelete() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            InOrder inOrder = inOrder(mediaHardDeleteHelper, mediaProviderService);
            inOrder.verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            inOrder.verify(mediaProviderService).purgeAllAssets(testMedia);
        }

        @Test
        @DisplayName("Should run Milvus delete after DB delete (best-effort)")
        void orchestration_milvusPurgeAfterDelete() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            InOrder inOrder = inOrder(mediaHardDeleteHelper, contentPipelineService);
            inOrder.verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            inOrder.verify(contentPipelineService).notifyMediaDeleted(testMediaId);
        }
    }

    @Nested
    @DisplayName("Best-Effort External Cleanup Resilience")
    class ExternalFailureResilienceTests {

        @Test
        @DisplayName("Should continue even if S3 purge fails")
        void s3PurgeFails_continueWithMilvus() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);
            doThrow(new RuntimeException("S3 error"))
                    .when(mediaProviderService).purgeAllAssets(testMedia);

            // Should not throw
            assertDoesNotThrow(() -> mediaPurgeService.purge(testMediaId, testAdminId, testReason));

            // DB delete should still have happened
            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            // Milvus notification should still be attempted
            verify(contentPipelineService).notifyMediaDeleted(testMediaId);
        }

        @Test
        @DisplayName("Should continue even if Milvus delete fails")
        void milvusPurgeFails_stillCompletes() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);
            doThrow(new RuntimeException("Milvus error"))
                    .when(contentPipelineService).notifyMediaDeleted(testMediaId);

            // Should not throw
            assertDoesNotThrow(() -> mediaPurgeService.purge(testMediaId, testAdminId, testReason));

            // DB and S3 should still have happened
            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            verify(mediaProviderService).purgeAllAssets(testMedia);
        }

        @Test
        @DisplayName("Should continue even if both S3 and Milvus fail")
        void bothS3AndMilvusFail_stillCompletes() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);
            doThrow(new RuntimeException("S3 error"))
                    .when(mediaProviderService).purgeAllAssets(testMedia);
            doThrow(new RuntimeException("Milvus error"))
                    .when(contentPipelineService).notifyMediaDeleted(testMediaId);

            // Should not throw
            assertDoesNotThrow(() -> mediaPurgeService.purge(testMediaId, testAdminId, testReason));

            // DB delete should still have happened
            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
        }
    }

    @Nested
    @DisplayName("Soft-Deleted Media Handling")
    class SoftDeletedMediaTests {

        @Test
        @DisplayName("Should allow purging of soft-deleted media")
        void softDeletedMedia_canBePurged() {
            // Media entity loads as-is (soft-deleted or not) via findByMediaId
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
        }
    }

    @Nested
    @DisplayName("Valid Purge Completion")
    class SuccessfulPurgeTests {

        @Test
        @DisplayName("Should complete purge successfully with all operations")
        void validPurge_allOperationsExecute() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            // Verify all operations were called in order
            verify(mediaRepository).findByMediaId(testMediaId);
            verify(mediaCopyrightRepository).existsBySourceMedia_MediaId(testMediaId);
            verify(playbackSecurityService).revokeActiveSessions(testMedia);
            verify(mediaHardDeleteHelper).hardDelete(testMedia, testAdminId, testReason);
            verify(mediaProviderService).purgeAllAssets(testMedia);
            verify(contentPipelineService).notifyMediaDeleted(testMediaId);
        }

        @Test
        @DisplayName("Should pass correct parameters to hard-delete helper")
        void validPurge_correctParametersToHelper() {
            when(mediaRepository.findByMediaId(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaCopyrightRepository.existsBySourceMedia_MediaId(testMediaId))
                    .thenReturn(false);

            mediaPurgeService.purge(testMediaId, testAdminId, testReason);

            verify(mediaHardDeleteHelper).hardDelete(
                    eq(testMedia),
                    eq(testAdminId),
                    eq(testReason));
        }
    }
}
