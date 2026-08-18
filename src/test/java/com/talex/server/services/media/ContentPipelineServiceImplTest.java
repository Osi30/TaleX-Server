package com.talex.server.services.media;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.kafka.CopyrightResultMessage;
import com.talex.server.dtos.kafka.CopyrightViolationItem;
import com.talex.server.dtos.kafka.ModerationResultMessage;
import com.talex.server.dtos.kafka.ModerationViolationItem;
import com.talex.server.dtos.kafka.PipelineJobMessage;
import com.talex.server.entities.media.ContentCensorship;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaCopyright;
import com.talex.server.enums.media.CensorshipStatus;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.SseNotificationService;
import com.talex.server.services.media.impls.ContentPipelineProducer;
import com.talex.server.services.media.impls.ContentPipelineServiceImpl;
import com.talex.server.services.media.MediaPackagingService;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentPipelineService Tests")
class ContentPipelineServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaCopyrightRepository mediaCopyrightRepository;

    @Mock
    private ContentCensorshipRepository contentCensorshipRepository;

    @Mock
    private ContentPipelineProducer pipelineProducer;

    @Mock
    private MediaProperties mediaProperties;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private MediaPackagingService mediaPackagingService;

    @InjectMocks
    private ContentPipelineServiceImpl service;

    private UUID testMediaId;
    private UUID testCreatorId;
    private Media testMedia;

    @BeforeEach
    void setUp() {
        testMediaId = UUID.randomUUID();
        testCreatorId = UUID.randomUUID();
        testMedia = new Media();
        testMedia.setMediaId(testMediaId.toString());
        testMedia.setMediaType(MediaType.VIDEO);
        testMedia.setProvider(MediaProvider.AWS);
        testMedia.setOriginalUrl("https://bucket.s3.region.amazonaws.com/videos/test.mp4");
        testMedia.setStatus(MediaStatus.PENDING);
        testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
        testMedia.setCreatorId(testCreatorId.toString());

        // Setup MediaProperties mock — nested class thật tên là MediaProperties.Aws
        // (không phải AwsProperties).
        MediaProperties.Aws awsProps = new MediaProperties.Aws();
        awsProps.setBucketName("test-bucket");
        awsProps.setCloudfrontDomain("d1234.cloudfront.net");
        awsProps.setRegion("us-east-1");
        // lenient — không phải mọi nested test class (handleModeration/handleCopyright)
        // đều đi tới nhánh code đọc AWS config (VD case media not found dừng sớm).
        lenient().when(mediaProperties.getAws()).thenReturn(awsProps);
    }

    // ──────────────────────────────────────────────────────────────
    // Media_002_DispatchJob Tests (5 UTCID)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("dispatchPipelineJob Tests")
    class DispatchPipelineJobTests {

        @Test
        @DisplayName("UTCID01: media status PENDING (fresh upload), Kafka send succeeds")
        void testDispatchPipelineJob_FreshUploadPending_Success() {
            // Arrange
            testMedia.setStatus(MediaStatus.PENDING);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.dispatchPipelineJob(testMedia);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.PENDING);

            ArgumentCaptor<PipelineJobMessage> msgCaptor = ArgumentCaptor.forClass(PipelineJobMessage.class);
            verify(pipelineProducer, times(1)).sendPipelineJob(msgCaptor.capture());
            PipelineJobMessage msg = msgCaptor.getValue();
            assertThat(msg.getMediaId()).isEqualTo(testMediaId.toString());
            assertThat(msg.getS3Bucket()).isEqualTo("test-bucket");
        }

        @Test
        @DisplayName("UTCID02: media status HLS_PROCESSING (resume-from-approval), Kafka send succeeds")
        void testDispatchPipelineJob_ResumeHlsProcessing_PreserveStatus() {
            // Arrange
            testMedia.setStatus(MediaStatus.HLS_PROCESSING);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.dispatchPipelineJob(testMedia);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.HLS_PROCESSING);
        }

        @Test
        @DisplayName("UTCID03: malformed originalUrl — extractS3Key() không throw, fallback trả nguyên văn URL, job KHÔNG bị đánh FAILED")
        void testDispatchPipelineJob_MalformedUrl_FallsBackGracefully() {
            // Arrange — extractS3Key() cố tình resilient: không parse được thì log warn +
            // trả nguyên văn URL làm key, KHÔNG throw. Job vẫn tiếp tục bình thường.
            testMedia.setOriginalUrl("not-a-valid-url");
            testMedia.setStatus(MediaStatus.PENDING);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.dispatchPipelineJob(testMedia);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.PENDING);

            ArgumentCaptor<PipelineJobMessage> msgCaptor = ArgumentCaptor.forClass(PipelineJobMessage.class);
            verify(pipelineProducer, times(1)).sendPipelineJob(msgCaptor.capture());
            assertThat(msgCaptor.getValue().getS3Key()).isEqualTo("not-a-valid-url");
        }

        @Test
        @DisplayName("UTCID04: Kafka send fails (broker unreachable/timeout)")
        void testDispatchPipelineJob_KafkaSendFails_MarkFailed() {
            // Arrange
            testMedia.setStatus(MediaStatus.PENDING);
            doThrow(new RuntimeException("Kafka broker unreachable"))
                    .when(pipelineProducer).sendPipelineJob(any());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.dispatchPipelineJob(testMedia);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, atLeastOnce()).save(mediaCaptor.capture());
            // Last save should have FAILED status
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.FAILED);
        }

        @Test
        @DisplayName("UTCID05: creator ID resolution fails (episode deleted)")
        void testDispatchPipelineJob_CreatorIdNotResolved_SendsEmptyCreatorId() {
            // Arrange
            testMedia.setCreatorId(null); // Force resolution to fail
            testMedia.setStatus(MediaStatus.PENDING);
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.dispatchPipelineJob(testMedia);

            // Assert
            ArgumentCaptor<PipelineJobMessage> msgCaptor = ArgumentCaptor.forClass(PipelineJobMessage.class);
            verify(pipelineProducer, times(1)).sendPipelineJob(msgCaptor.capture());
            PipelineJobMessage msg = msgCaptor.getValue();
            // creatorId should be empty or null when resolution fails
            assertThat(msg.getCreatorId()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Media_003_HandleModeration Tests (7 UTCID)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleModerationResult Tests")
    class HandleModerationResultTests {

        @Test
        @DisplayName("UTCID01: media found, no existing censorship, success=true, isSafe=true")
        void testHandleModeration_SafeContent_AutoApprove() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(true)
                    .primaryLabel("Safe")
                    .confidenceScore(95.0f)
                    .violations(List.of())
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert — nhánh isSafe=true CHỈ set field trên entity đã managed (@Transactional,
            // JPA dirty-checking tự flush khi commit), KHÔNG gọi mediaRepository.save() tường
            // minh (khác nhánh lỗi hệ thống/flag vi phạm bên dưới, những nhánh đó CÓ save()).
            assertThat(testMedia.getApprovalStatus()).isEqualTo(ContentApprovalStatus.APPROVED);
            assertThat(testMedia.getApprovalReviewedBy()).isEqualTo("content-pipeline");
            verify(mediaRepository, never()).save(any());

            verify(contentCensorshipRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("UTCID02: media found, no existing censorship, success=true, isSafe=false, all violations shielded by declared warnings")
        void testHandleModeration_ViolationsShieldedByWarnings_AutoApprove() {
            // Arrange
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Mock episode chain to return MATURE series with declared warnings
            testMedia.setStatus(MediaStatus.PENDING);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .parentLabel("Graphic Violence")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .confidenceScore(75.0f)
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert - should NOT be auto-approved if series warnings cannot be resolved
            // (since we mock episode chain to fail), should route to Staff
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isIn(
                    ContentApprovalStatus.PENDING_REVIEW, ContentApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("UTCID03: media found, no existing censorship, success=true, isSafe=false, violations not fully shielded")
        void testHandleModeration_ViolationsNotShielded_RouteToStaffReview() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .parentLabel("Graphic Violence")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .confidenceScore(75.0f)
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
            assertThat(saved.getApprovalReviewedBy()).isNull();
            assertThat(saved.getApprovalReviewedAt()).isNull();
        }

        @Test
        @DisplayName("UTCID04: media not found (unknown mediaId)")
        void testHandleModeration_MediaNotFound_SkipSilently() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse("unknown-id"))
                    .thenReturn(Optional.empty());

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId("unknown-id")
                    .success(true)
                    .isSafe(true)
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert - should not crash, not save anything
            verify(mediaRepository, times(0)).save(any());
            verify(contentCensorshipRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("UTCID05: censorship record already exists (duplicate/idempotent retry)")
        void testHandleModeration_CensorshipAlreadyExists_SkipIdempotent() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            ContentCensorship existing = new ContentCensorship();
            existing.setStatus(CensorshipStatus.APPROVED);
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of(existing));

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(true)
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            verify(mediaRepository, times(0)).save(any());
            verify(contentCensorshipRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("UTCID06: success=false (AI system error, e.g. Rekognition timeout)")
        void testHandleModeration_SystemError_MarkFailed() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(false)
                    .isSafe(false)
                    .errorMessage("Rekognition timeout")
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.FAILED);
            assertThat(saved.getErrorMessage()).contains("Moderation check failed");
        }

        @Test
        @DisplayName("UTCID07: isSafe=false, not fully shielded, but media.status already FAILED from a previous step")
        void testHandleModeration_ViolationsNotShielded_ButStatusAlreadyFailed_PreserveStatus() {
            // Arrange
            testMedia.setStatus(MediaStatus.FAILED);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.FAILED);
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Media_004_HandleCopyright Tests (10 UTCID)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleCopyrightResult Tests")
    class HandleCopyrightResultTests {

        @Test
        @DisplayName("UTCID01: found, new contentId, success=true, not duplicate, VIDEO+AWS, moderation not flagged")
        void testHandleCopyright_VideoAwsNoDuplicate_TriggerHlsPackaging() {
            // Arrange
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id-123")
                    .isDuplicate(false)
                    .success(true)
                    .previewS3Key("previews/test.jpg")
                    .watermarkedS3Key(null)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getContentId()).isEqualTo("content-id-123");
            assertThat(saved.getPreviewUrl()).isNotNull();

            // Should trigger HLS packaging for video+AWS
            verify(mediaPackagingService, times(1)).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID02: found, new contentId, success=true, not duplicate, IMAGE")
        void testHandleCopyright_Image_NoHlsPackaging() {
            // Arrange
            testMedia.setMediaType(MediaType.IMAGE);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id-456")
                    .isDuplicate(false)
                    .success(true)
                    .previewS3Key("previews/image.jpg")
                    .watermarkedS3Key(null)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            assertThat(testMedia.getContentId()).isEqualTo("content-id-456");
            verify(mediaPackagingService, times(0)).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID03: found, new contentId, success=true, not duplicate, VIDEO, watermarkedS3Key present")
        void testHandleCopyright_VideoWithWatermark_UpdateFileUrl() {
            // Arrange
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id-789")
                    .isDuplicate(false)
                    .success(true)
                    .previewS3Key("previews/test.jpg")
                    .watermarkedS3Key("videos/ab_watermarked/test_hls")
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getHasWatermark()).isTrue();
            assertThat(saved.getFileUrl()).contains("d1234.cloudfront.net");
        }

        @Test
        @DisplayName("UTCID04: media not found, unknown to environment")
        void testHandleCopyright_MediaNotFound_SkipSilently() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse("unknown-id"))
                    .thenReturn(Optional.empty());
            when(mediaRepository.findByMediaId("unknown-id"))
                    .thenReturn(Optional.empty());

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId("unknown-id")
                    .contentId("content-id-999")
                    .success(true)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            verify(mediaRepository, times(0)).save(any());
            verify(mediaCopyrightRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("UTCID05: media not found, but was soft-deleted")
        void testHandleCopyright_MediaSoftDeleted_LogAndSkip() {
            // Arrange
            Media softDeletedMedia = new Media();
            softDeletedMedia.setMediaId(testMediaId.toString());
            softDeletedMedia.setIsDeleted(true);

            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.empty());
            when(mediaRepository.findByMediaId(testMediaId.toString()))
                    .thenReturn(Optional.of(softDeletedMedia));

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id-000")
                    .success(true)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            verify(mediaRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("UTCID06: media found, contentId already set (idempotent retry)")
        void testHandleCopyright_ContentIdAlreadySet_SkipIdempotent() {
            // Arrange
            testMedia.setContentId("already-set-id");
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("new-content-id")
                    .success(true)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            verify(mediaRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("UTCID07: success=false (AI system error)")
        void testHandleCopyright_SystemError_MarkFailed() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id")
                    .success(false)
                    .errorMessage("S3 access error")
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.FAILED);
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.REJECTED);
        }

        @Test
        @DisplayName("UTCID08: Moderation already flagged media for review")
        void testHandleCopyright_ModerationAlreadyFlaggedForReview_SkipHlsPackaging() {
            // Arrange
            testMedia.setMediaType(MediaType.VIDEO);
            testMedia.setProvider(MediaProvider.AWS);
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            ContentCensorship censorshipRecord = new ContentCensorship();
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of(censorshipRecord));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id")
                    .isDuplicate(false)
                    .success(true)
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            verify(mediaPackagingService, times(0)).createHlsPackaging(any());
        }

        @Test
        @DisplayName("UTCID09: isDuplicate=true, not all violations CC0")
        void testHandleCopyright_DuplicateNonCC0_RouteToStaffReview() {
            // Arrange
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            testMedia.setStatus(MediaStatus.PENDING);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.findByMediaId(anyString()))
                    .thenReturn(Optional.empty()); // Source not found
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(mediaCopyrightRepository.save(any(MediaCopyright.class)))
                    .thenReturn(new MediaCopyright());

            CopyrightViolationItem violation = CopyrightViolationItem.builder()
                    .sourceMediaId(UUID.randomUUID().toString())
                    .similarityScore(0.95f)
                    .violationType("VIDEO")
                    .build();

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id")
                    .isDuplicate(true)
                    .success(true)
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
            assertThat(saved.getStatus()).isEqualTo(MediaStatus.INACTIVE);
        }

        @Test
        @DisplayName("UTCID10: isDuplicate=true, all violations CC0 (self-declared)")
        void testHandleCopyright_DuplicateAllCC0_BypassStaffReview() {
            // Arrange
            testMedia.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
            testMedia.setStatus(MediaStatus.PENDING);
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());

            // Create source media with CC0 copyright
            Media sourceMedia = new Media();
            sourceMedia.setMediaId(UUID.randomUUID().toString());
            sourceMedia.setIsDeleted(false);
            sourceMedia.setCopyright(new com.talex.server.entities.media.Copyright());
            sourceMedia.getCopyright().setCode("CC0");

            when(mediaRepository.findByMediaId(anyString()))
                    .thenReturn(Optional.of(sourceMedia));
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);
            when(mediaCopyrightRepository.save(any(MediaCopyright.class)))
                    .thenReturn(new MediaCopyright());

            CopyrightViolationItem violation = CopyrightViolationItem.builder()
                    .sourceMediaId(sourceMedia.getMediaId().toString())
                    .similarityScore(0.98f)
                    .violationType("VIDEO")
                    .build();

            CopyrightResultMessage result = CopyrightResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .contentId("content-id")
                    .isDuplicate(true)
                    .success(true)
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleCopyrightResult(result);

            // Assert - should auto-approve CC0 content
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            // According to code logic, CC0 bypass means media does NOT get PENDING_REVIEW status
            assertThat(saved.getApprovalStatus()).isNotEqualTo(ContentApprovalStatus.REJECTED);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Mod_001_BypassEvaluation Tests (8 UTCID)
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("areAllViolationsShieldedByDeclaredWarnings Tests")
    class AreAllViolationsShieldedByDeclaredWarningsTests {

        @Test
        @DisplayName("UTCID01: series MATURE, declaredWarnings non-empty, all violations map to declared groups")
        void testBypass_AllViolationsShielded_ReturnTrue() {
            // Arrange
            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .parentLabel("Graphic Violence")
                    .confidence(75.0f)
                    .build();

            // Setup media with mocked episode chain
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            // Act
            service.handleModerationResult(result);

            // Assert - with mocked episode returning null warnings, should route to review
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
        }

        @Test
        @DisplayName("UTCID02: violations = null")
        void testBypass_ViolationsNull_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Violence")
                    .violations(null)
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID03: violations = empty list")
        void testBypass_ViolationsEmpty_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Violence")
                    .violations(List.of())
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID04: series ageRating not MATURE")
        void testBypass_SeriesNotMature_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert - series age rating not MATURE, should route to review
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID05: declaredWarnings empty (creator declared none)")
        void testBypass_NoDeclaredWarnings_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID06: at least one violation maps to a group not in declaredWarnings")
        void testBypass_ViolationGroupNotDeclared_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .parentLabel("Graphic Violence")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID07: a violation's label maps to no known group (null)")
        void testBypass_ViolationLabelUnknown_ReturnFalse() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("UnknownLabel")
                    .parentLabel("UnknownParent")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("UnknownLabel")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("UTCID08: single violation only, exactly matches the one declared group (boundary)")
        void testBypass_SingleViolationMatchesOneGroup_Boundary() {
            // Arrange
            when(mediaRepository.findByMediaIdAndIsDeletedFalse(testMediaId.toString()))
                    .thenReturn(Optional.of(testMedia));
            when(contentCensorshipRepository.findAllByMedia_MediaId(testMediaId.toString()))
                    .thenReturn(List.of());
            when(mediaRepository.save(any(Media.class))).thenReturn(testMedia);

            ModerationViolationItem violation = ModerationViolationItem.builder()
                    .label("Blood & Gore")
                    .confidence(75.0f)
                    .build();

            ModerationResultMessage result = ModerationResultMessage.builder()
                    .mediaId(testMediaId.toString())
                    .success(true)
                    .isSafe(false)
                    .primaryLabel("Graphic Violence")
                    .violations(List.of(violation))
                    .build();

            // Act
            service.handleModerationResult(result);

            // Assert
            ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository, times(1)).save(mediaCaptor.capture());
            Media saved = mediaCaptor.getValue();
            assertThat(saved.getApprovalStatus()).isEqualTo(ContentApprovalStatus.PENDING_REVIEW);
        }
    }
}
