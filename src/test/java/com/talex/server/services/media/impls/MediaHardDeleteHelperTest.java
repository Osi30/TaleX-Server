package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaPurgeLog;
import com.talex.server.entities.series.Episode;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaPlaybackSessionRepository;
import com.talex.server.repositories.media.MediaPurgeLogRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.media.MediaUploadSessionRepository;
import com.talex.server.repositories.media.ViolationDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MediaHardDeleteHelper}.
 *
 * Tests the irreversible hard-delete logic: audit log creation, FK-safe deletion order,
 * and verification that all child tables are properly cleaned.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaHardDeleteHelper")
class MediaHardDeleteHelperTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaPurgeLogRepository mediaPurgeLogRepository;

    @Mock
    private MediaCopyrightRepository mediaCopyrightRepository;

    @Mock
    private ContentCensorshipRepository contentCensorshipRepository;

    @Mock
    private ViolationDetailRepository violationDetailRepository;

    @Mock
    private MediaPlaybackSessionRepository mediaPlaybackSessionRepository;

    @Mock
    private MediaUploadSessionRepository mediaUploadSessionRepository;

    @InjectMocks
    private MediaHardDeleteHelper mediaHardDeleteHelper;

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
    @DisplayName("Audit Log Creation")
    class AuditLogCreationTests {

        @Test
        @DisplayName("Should create audit log with correct media ID")
        void auditLog_correctMediaId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals(testMediaId, savedLog.getMediaId());
        }

        @Test
        @DisplayName("Should create audit log with episode ID from media")
        void auditLog_correctEpisodeId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals("episode-001", savedLog.getEpisodeId());
        }

        @Test
        @DisplayName("Should create audit log with creator ID from media")
        void auditLog_correctCreatorId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals("creator-001", savedLog.getCreatorId());
        }

        @Test
        @DisplayName("Should create audit log with file URL")
        void auditLog_correctFileUrl() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals("s3://talex-media/media-001.mp4", savedLog.getFileUrl());
        }

        @Test
        @DisplayName("Should create audit log with provider public ID")
        void auditLog_correctProviderPublicId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals("provider-id-001", savedLog.getProviderPublicId());
        }

        @Test
        @DisplayName("Should create audit log with file size")
        void auditLog_correctFileSize() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals(1024000L, savedLog.getFileSize());
        }

        @Test
        @DisplayName("Should create audit log with admin account ID")
        void auditLog_correctAdminId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals(testAdminId, savedLog.getAdminAccountId());
        }

        @Test
        @DisplayName("Should create audit log with purge reason")
        void auditLog_correctReason() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertEquals(testReason, savedLog.getReason());
        }

        @Test
        @DisplayName("Should handle media with no episode gracefully")
        void auditLog_mediaWithoutEpisode() {
            testMedia.setEpisode(null);

            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            ArgumentCaptor<MediaPurgeLog> logCaptor = ArgumentCaptor.forClass(MediaPurgeLog.class);
            verify(mediaPurgeLogRepository).save(logCaptor.capture());

            MediaPurgeLog savedLog = logCaptor.getValue();
            assertNull(savedLog.getEpisodeId());
        }
    }

    @Nested
    @DisplayName("FK-Safe Deletion Order")
    class DeletionOrderTests {

        @Test
        @DisplayName("Should write audit log BEFORE any child table deletions")
        void deletionOrder_auditLogFirst() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    mediaPurgeLogRepository,
                    violationDetailRepository,
                    contentCensorshipRepository,
                    mediaCopyrightRepository,
                    mediaUploadSessionRepository,
                    mediaPlaybackSessionRepository,
                    mediaRepository
            );

            // Audit log must be persisted first
            inOrder.verify(mediaPurgeLogRepository).save(any(MediaPurgeLog.class));
            inOrder.verify(violationDetailRepository).deleteAllByCensorshipMediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete violation_detail BEFORE content_censorship")
        void deletionOrder_violationDetailBeforeCensorship() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    violationDetailRepository,
                    contentCensorshipRepository
            );

            inOrder.verify(violationDetailRepository).deleteAllByCensorshipMediaId(testMediaId);
            inOrder.verify(contentCensorshipRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete content_censorship BEFORE media_copyright")
        void deletionOrder_censorshipBeforeCopyright() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    contentCensorshipRepository,
                    mediaCopyrightRepository
            );

            inOrder.verify(contentCensorshipRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaCopyrightRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete media_copyright BEFORE media_upload_sessions")
        void deletionOrder_copyrightBeforeUploadSessions() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    mediaCopyrightRepository,
                    mediaUploadSessionRepository
            );

            inOrder.verify(mediaCopyrightRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaUploadSessionRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete media_upload_sessions BEFORE media_playback_sessions")
        void deletionOrder_uploadSessionsBeforePlaybackSessions() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    mediaUploadSessionRepository,
                    mediaPlaybackSessionRepository
            );

            inOrder.verify(mediaUploadSessionRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaPlaybackSessionRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete media_playback_sessions BEFORE media row")
        void deletionOrder_playbackSessionsBeforeMediaRow() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    mediaPlaybackSessionRepository,
                    mediaRepository
            );

            inOrder.verify(mediaPlaybackSessionRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaRepository).delete(testMedia);
        }

        @Test
        @DisplayName("Should delete media row LAST")
        void deletionOrder_mediaRowLast() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            InOrder inOrder = inOrder(
                    violationDetailRepository,
                    contentCensorshipRepository,
                    mediaCopyrightRepository,
                    mediaUploadSessionRepository,
                    mediaPlaybackSessionRepository,
                    mediaRepository
            );

            inOrder.verify(violationDetailRepository).deleteAllByCensorshipMediaId(testMediaId);
            inOrder.verify(contentCensorshipRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaCopyrightRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaUploadSessionRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaPlaybackSessionRepository).deleteAllByMedia_MediaId(testMediaId);
            inOrder.verify(mediaRepository).delete(testMedia);
        }
    }

    @Nested
    @DisplayName("Child Table Deletion")
    class ChildTableDeletionTests {

        @Test
        @DisplayName("Should delete all violation details for the media")
        void childTable_violationDetailsDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(violationDetailRepository).deleteAllByCensorshipMediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete all content censorship records for the media")
        void childTable_censorshipDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(contentCensorshipRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete media copyright records where media is TARGET")
        void childTable_mediaCopyrightDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(mediaCopyrightRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete upload sessions")
        void childTable_uploadSessionsDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(mediaUploadSessionRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete playback sessions")
        void childTable_playbackSessionsDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(mediaPlaybackSessionRepository).deleteAllByMedia_MediaId(testMediaId);
        }

        @Test
        @DisplayName("Should delete media row")
        void childTable_mediaRowDeleted() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(mediaRepository).delete(testMedia);
        }
    }

    @Nested
    @DisplayName("Correct Parameter Passing")
    class ParameterPassingTests {

        @Test
        @DisplayName("Should pass correct media ID to all delete methods")
        void parameters_correctMediaId() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(violationDetailRepository).deleteAllByCensorshipMediaId(eq(testMediaId));
            verify(contentCensorshipRepository).deleteAllByMedia_MediaId(eq(testMediaId));
            verify(mediaCopyrightRepository).deleteAllByMedia_MediaId(eq(testMediaId));
            verify(mediaUploadSessionRepository).deleteAllByMedia_MediaId(eq(testMediaId));
            verify(mediaPlaybackSessionRepository).deleteAllByMedia_MediaId(eq(testMediaId));
        }

        @Test
        @DisplayName("Should pass correct media entity to repository delete")
        void parameters_correctMediaEntity() {
            mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason);

            verify(mediaRepository).delete(eq(testMedia));
        }
    }

    @Nested
    @DisplayName("Idempotency and Error Handling")
    class IdempotencyTests {

        @Test
        @DisplayName("Should handle successful purge without exceptions")
        void noException_successfulPurge() {
            assertDoesNotThrow(() ->
                    mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason)
            );
        }

        @Test
        @DisplayName("Should propagate exceptions from repository operations")
        void exception_repositoryFailure() {
            doThrow(new RuntimeException("Database error"))
                    .when(mediaPurgeLogRepository).save(any(MediaPurgeLog.class));

            assertThrows(RuntimeException.class, () ->
                    mediaHardDeleteHelper.hardDelete(testMedia, testAdminId, testReason)
            );
        }
    }
}
