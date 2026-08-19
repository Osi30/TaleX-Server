package com.talex.server.services.media.impls;

import com.talex.server.dtos.requests.media.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.media.MediaRejectRequestDto;
import com.talex.server.dtos.responses.media.MediaResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.media.ContentCensorship;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.enums.media.CensorshipStatus;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.media.ContentPipelineService;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaServiceImpl Tests")
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private MediaProviderService mediaProviderService;

    @Mock
    private MediaPackagingService mediaPackagingService;

    @Mock
    private MediaPlaybackSecurityService playbackSecurityService;

    @Mock
    private ContentPipelineService contentPipelineService;

    @Mock
    private ContentCensorshipRepository contentCensorshipRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ContentOwnershipService contentOwnershipService;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Episode testEpisode;
    private Media testMedia;
    private String testEpisodeId;
    private String testMediaId;
    private String testAccountId;
    private String testCreatorId;

    @BeforeEach
    void setUp() {
        testEpisodeId = UUID.randomUUID().toString();
        testMediaId = UUID.randomUUID().toString();
        testAccountId = UUID.randomUUID().toString();
        testCreatorId = UUID.randomUUID().toString();

        // Setup test episode
        testEpisode = new Episode();
        testEpisode.setEpisodeId(testEpisodeId);
        testEpisode.setCreatorId(testCreatorId);
        testEpisode.setStatus(EpisodeStatus.DRAFT);
        testEpisode.setContentType(ContentType.VIDEO);

        // Setup test media
        testMedia = new Media();
        testMedia.setMediaId(testMediaId);
        testMedia.setEpisode(testEpisode);
        testMedia.setCreatorId(testCreatorId);
        testMedia.setMediaType(MediaType.VIDEO);
        testMedia.setStatus(MediaStatus.PENDING);
        testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
    }

    // ====================== NESTED TEST CLASSES FOR createFromUrl ======================

    @Nested
    @DisplayName("createFromUrl() Tests")
    class CreateFromUrlTests {

        @Test
        @DisplayName("UTCID01: Valid IMAGE media — creates media and dispatches pipeline job")
        void testCreateFromUrlValidImageMedia() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);
            request.setChecksum("6105d6cc76af400325e94d588ce511be5bfdbb73b437dc51eca43917d7a43e3d");
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);
            request.setStorageProvider("URL");

            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);
            when(mediaRepository.findFirstByChecksumAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
            when(mediaRepository.findMaxDisplayOrderByEpisodeId(testEpisodeId)).thenReturn(0);

            Media savedMedia = new Media();
            savedMedia.setMediaId(testMediaId);
            savedMedia.setEpisode(testEpisode);
            savedMedia.setCreatorId(testCreatorId);
            savedMedia.setMediaType(MediaType.IMAGE);
            savedMedia.setStatus(MediaStatus.ACTIVE);
            savedMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
            doNothing().when(contentPipelineService).dispatchPipelineJob(savedMedia);

            // Act
            MediaResponseDto response = mediaService.createFromUrl(testEpisodeId, request, testAccountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMediaId()).isEqualTo(testMediaId);
            assertThat(response.getMediaType()).isEqualTo(MediaType.IMAGE);
            assertThat(response.getStatus()).isEqualTo(MediaStatus.ACTIVE);
            verify(contentPipelineService, times(1)).dispatchPipelineJob(savedMedia);
            verify(mediaRepository, times(1)).save(any(Media.class));
        }

        @Test
        @DisplayName("UTCID02: Valid VIDEO media — creates media without pipeline dispatch")
        void testCreateFromUrlValidVideoMedia() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/video.mp4");
            request.setMimeType("video/mp4");
            request.setFileSize(5242880L);
            request.setChecksum("0cab1c9617404faf2b24e221e189ca5945813e14d3f766345b09ca13bbe28ffc");
            request.setMediaType(MediaType.VIDEO);
            request.setActorId(testAccountId);
            request.setStorageProvider("URL");

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);
            when(mediaRepository.findFirstByChecksumAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
            when(mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                    eq(testEpisodeId), eq(MediaType.VIDEO), any())).thenReturn(false);

            Media savedMedia = new Media();
            savedMedia.setMediaId(testMediaId);
            savedMedia.setEpisode(testEpisode);
            savedMedia.setCreatorId(testCreatorId);
            savedMedia.setMediaType(MediaType.VIDEO);
            savedMedia.setStatus(MediaStatus.ACTIVE);

            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

            // Act
            MediaResponseDto response = mediaService.createFromUrl(testEpisodeId, request, testAccountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMediaType()).isEqualTo(MediaType.VIDEO);
            verify(contentPipelineService, never()).dispatchPipelineJob(any());
        }

        @Test
        @DisplayName("UTCID03: Episode not found — throws ContentModuleException")
        void testCreateFromUrlEpisodeNotFound() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class);
        }

        @Test
        @DisplayName("UTCID04: Account does not own/manage episode — throws permission denied")
        void testCreateFromUrlAccountCannotManage() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);

            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doThrow(new ContentModuleException(4002, null, "Permission denied", null))
                    .when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class);
        }

        @Test
        @DisplayName("UTCID05: Episode status does not allow modification — throws bad request")
        void testCreateFromUrlInvalidEpisodeStatus() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);

            testEpisode.setStatus(EpisodeStatus.PUBLISHED);
            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("Cannot modify media");
        }

        @Test
        @DisplayName("UTCID06: Request is null — throws validation error")
        void testCreateFromUrlNullRequest() {
            // Arrange
            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, null, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("Media URL request is required");
        }

        @Test
        @DisplayName("UTCID07: Invalid mimeType for media type — throws validation error")
        void testCreateFromUrlInvalidMimeType() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/file.txt");
            request.setMimeType("text/plain"); // Invalid for IMAGE
            request.setFileSize(1024L);
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);

            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("mimeType must be an image MIME type");
        }

        @Test
        @DisplayName("UTCID08: Invalid fileSize (negative) — throws validation error")
        void testCreateFromUrlNegativeFileSize() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(-1L); // Invalid
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);

            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("fileSize must be zero or positive");
        }

        @Test
        @DisplayName("UTCID09: Duplicate checksum — throws conflict error")
        void testCreateFromUrlDuplicateChecksum() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);
            request.setChecksum("6105d6cc76af400325e94d588ce511be5bfdbb73b437dc51eca43917d7a43e3d");
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);

            testEpisode.setContentType(ContentType.COMIC);

            Media existingMedia = new Media();
            existingMedia.setMediaId(UUID.randomUUID().toString());

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);
            when(mediaRepository.findFirstByChecksumAndIsDeletedFalse(
                    "6105d6cc76af400325e94d588ce511be5bfdbb73b437dc51eca43917d7a43e3d"))
                    .thenReturn(Optional.of(existingMedia));

            // Act & Assert
            assertThatThrownBy(() -> mediaService.createFromUrl(testEpisodeId, request, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("Duplicate media URL detected");
        }

        @Test
        @DisplayName("UTCID10: Pipeline dispatch fails but media saved — does not throw")
        void testCreateFromUrlPipelineDispatchFails() {
            // Arrange
            MediaMetadataRequestDto request = new MediaMetadataRequestDto();
            request.setFileUrl("https://example.com/image.png");
            request.setMimeType("image/png");
            request.setFileSize(1024L);
            request.setChecksum("6105d6cc76af400325e94d588ce511be5bfdbb73b437dc51eca43917d7a43e3d");
            request.setMediaType(MediaType.IMAGE);
            request.setActorId(testAccountId);
            request.setStorageProvider("URL");

            testEpisode.setContentType(ContentType.COMIC);

            when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(testEpisodeId))
                    .thenReturn(Optional.of(testEpisode));
            doNothing().when(contentOwnershipService).assertCanManage(testEpisode, testAccountId);
            when(mediaRepository.findFirstByChecksumAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
            when(mediaRepository.findMaxDisplayOrderByEpisodeId(testEpisodeId)).thenReturn(0);

            Media savedMedia = new Media();
            savedMedia.setMediaId(testMediaId);
            savedMedia.setEpisode(testEpisode);
            savedMedia.setCreatorId(testCreatorId);
            savedMedia.setMediaType(MediaType.IMAGE);
            savedMedia.setStatus(MediaStatus.ACTIVE);
            savedMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
            doThrow(new RuntimeException("Kafka error"))
                    .when(contentPipelineService).dispatchPipelineJob(savedMedia);

            // Act — should not throw
            MediaResponseDto response = mediaService.createFromUrl(testEpisodeId, request, testAccountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMediaId()).isEqualTo(testMediaId);
        }
    }

    // ====================== NESTED TEST CLASSES FOR approve ======================

    @Nested
    @DisplayName("approve() Tests")
    class ApproveTests {

        @Test
        @DisplayName("UTCID01: Media status ACTIVE (not INACTIVE) — changes approval, status unchanged")
        void testApproveMediaAlreadyActive() {
            // Arrange
            testMedia.setStatus(MediaStatus.ACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            MediaResponseDto response = mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.ACTIVE);
            assertThat(testMedia.getApprovalReviewedBy()).isEqualTo(testAccountId);
            assertThat(testMedia.getApprovalReviewedAt()).isNotNull();
            verify(mediaPackagingService, never()).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID02: Media status INACTIVE, IMAGE type — activates immediately")
        void testApproveInactiveImageMedia() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setMediaType(MediaType.IMAGE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.ACTIVE);
            verify(mediaPackagingService, never()).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID03: Media INACTIVE, VIDEO, providerAssetId=null, hlsReadyAt=null — resumes HLS packaging")
        void testApproveInactiveVideoNeverSubmitted() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setProviderAssetId(null);
            testMedia.setHlsReadyAt(null);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaPackagingService.createHlsPackaging(testMedia)).thenReturn("provider-asset-id-123");
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            // Status stays INACTIVE because transcode not finished
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.INACTIVE);
            verify(mediaPackagingService, times(1)).createHlsPackaging(testMedia);
        }

        @Test
        @DisplayName("UTCID04: Media INACTIVE, VIDEO, providerAssetId set — transcode in progress, waiting")
        void testApproveInactiveVideoTranscodeInProgress() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setProviderAssetId("asset-123");
            testMedia.setHlsReadyAt(null);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.INACTIVE);
            verify(mediaPackagingService, never()).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID05: Media INACTIVE, VIDEO, hlsReadyAt set — transcode finished, activates")
        void testApproveInactiveVideoTranscodeComplete() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setProviderAssetId("asset-123");
            testMedia.setHlsReadyAt(LocalDateTime.now().minusHours(1));
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.ACTIVE);
            verify(mediaPackagingService, never()).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID06: Media not found — throws not found exception")
        void testApproveMediaNotFound() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mediaService.approve(testMediaId, testAccountId))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("Media not found");
        }

        @Test
        @DisplayName("UTCID07: Media INACTIVE, VIDEO, provider not AWS — does not resume packaging")
        void testApproveInactiveVideoNonAwsProvider() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.URL); // Not AWS
            testMedia.setProviderAssetId(null);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            mediaService.approve(testMediaId, testAccountId);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.INACTIVE);
            verify(mediaPackagingService, never()).createHlsPackaging(any());
        }
    }

    // ====================== NESTED TEST CLASSES FOR rejectWithReason ======================

    @Nested
    @DisplayName("rejectWithReason() Tests")
    class RejectWithReasonTests {

        @Test
        @DisplayName("UTCID01: Media ACTIVE, reason provided — hides, revokes sessions, records censorship")
        void testRejectWithReasonMediaActive() {
            // Arrange
            testMedia.setStatus(MediaStatus.ACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason("Bạo lực quá mức");

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            doNothing().when(playbackSecurityService).revokeActiveSessions(testMedia);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(testMedia, "Bạo lực quá mức");

            // Act
            MediaResponseDto response = mediaService.rejectWithReason(testMediaId, testAccountId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.REJECTED);
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.HIDDEN);
            assertThat(testMedia.getApprovalReviewedBy()).isEqualTo(testAccountId);
            assertThat(testMedia.getApprovalReviewedAt()).isNotNull();
            verify(playbackSecurityService, times(1)).revokeActiveSessions(testMedia);
            verify(contentCensorshipRepository, times(1)).save(any(ContentCensorship.class));
            verify(contentPipelineService, times(1)).notifyStaffRejected(testMedia, "Bạo lực quá mức");
        }

        @Test
        @DisplayName("UTCID02: Media HLS_READY, reason provided — hides and revokes sessions")
        void testRejectWithReasonMediaHlsReady() {
            // Arrange
            testMedia.setStatus(MediaStatus.HLS_READY);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason("Nội dung giả mạo");

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            doNothing().when(playbackSecurityService).revokeActiveSessions(testMedia);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(any(), anyString());

            // Act
            mediaService.rejectWithReason(testMediaId, testAccountId, request);

            // Assert
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.HIDDEN);
            verify(playbackSecurityService, times(1)).revokeActiveSessions(testMedia);
        }

        @Test
        @DisplayName("UTCID03: Media INACTIVE — does not revoke (status not ACTIVE/HLS_READY)")
        void testRejectWithReasonMediaInactive() {
            // Arrange
            testMedia.setStatus(MediaStatus.INACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason("Nội dung nhạy cảm");

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(any(), anyString());

            // Act
            mediaService.rejectWithReason(testMediaId, testAccountId, request);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.REJECTED);
            // Status unchanged from INACTIVE
            assertThat(testMedia.getStatus()).isEqualTo(MediaStatus.INACTIVE);
            verify(playbackSecurityService, never()).revokeActiveSessions(any());
        }

        @Test
        @DisplayName("UTCID04: Request is null — uses empty string as reason")
        void testRejectWithReasonNullRequest() {
            // Arrange
            testMedia.setStatus(MediaStatus.ACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            doNothing().when(playbackSecurityService).revokeActiveSessions(testMedia);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(any(), anyString());

            // Act
            mediaService.rejectWithReason(testMediaId, testAccountId, null);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.REJECTED);
            ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
            verify(contentPipelineService, times(1)).notifyStaffRejected(eq(testMedia), reasonCaptor.capture());
            assertThat(reasonCaptor.getValue()).isEqualTo("");
        }

        @Test
        @DisplayName("UTCID05: Request present but reason field null — uses empty string")
        void testRejectWithReasonNullReasonField() {
            // Arrange
            testMedia.setStatus(MediaStatus.ACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason(null);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            doNothing().when(playbackSecurityService).revokeActiveSessions(testMedia);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(any(), anyString());

            // Act
            mediaService.rejectWithReason(testMediaId, testAccountId, request);

            // Assert
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.REJECTED);
            ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
            verify(contentPipelineService, times(1)).notifyStaffRejected(eq(testMedia), reasonCaptor.capture());
            assertThat(reasonCaptor.getValue()).isEqualTo("");
        }

        @Test
        @DisplayName("UTCID06: Media not found — throws not found exception")
        void testRejectWithReasonMediaNotFound() {
            // Arrange
            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason("Test reason");

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mediaService.rejectWithReason(testMediaId, testAccountId, request))
                    .isInstanceOf(ContentModuleException.class)
                    .hasMessageContaining("Media not found");
        }

        @Test
        @DisplayName("Records ContentCensorship with correct fields")
        void testRejectWithReasonRecordsCensorshipEntry() {
            // Arrange
            testMedia.setStatus(MediaStatus.ACTIVE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            MediaRejectRequestDto request = new MediaRejectRequestDto();
            request.setReason("Spam content");

            ArgumentCaptor<ContentCensorship> censorshipCaptor = ArgumentCaptor.forClass(ContentCensorship.class);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId))
                    .thenReturn(Optional.of(testMedia));
            doNothing().when(playbackSecurityService).revokeActiveSessions(testMedia);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(contentCensorshipRepository.save(any(ContentCensorship.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(contentPipelineService).notifyStaffRejected(any(), anyString());

            // Act
            mediaService.rejectWithReason(testMediaId, testAccountId, request);

            // Assert
            verify(contentCensorshipRepository, times(1)).save(censorshipCaptor.capture());
            ContentCensorship captured = censorshipCaptor.getValue();
            assertThat(captured.getMedia()).isEqualTo(testMedia);
            assertThat(captured.getReviewedBy()).isEqualTo("HUMAN");
            assertThat(captured.getReviewerNotes()).isEqualTo("Spam content");
            assertThat(captured.getStatus()).isEqualTo(CensorshipStatus.REJECTED);
            assertThat(captured.getCheckedAt()).isNotNull();
        }
    }
}
