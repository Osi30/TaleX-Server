package com.talex.server.services.media.impls;

import com.talex.server.dtos.requests.media.MediaComicPageRequestDto;
import com.talex.server.dtos.requests.media.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.media.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.media.MediaRejectRequestDto;
import com.talex.server.dtos.requests.media.MediaReorderRequestDto;
import com.talex.server.dtos.requests.media.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.media.ContentCensorshipResponseDto;
import com.talex.server.dtos.responses.media.MediaCopyrightResponseDto;
import com.talex.server.dtos.responses.media.MediaResponseDto;
import com.talex.server.dtos.responses.media.MediaSystemConfigResponseDto;
import com.talex.server.dtos.responses.media.MediaViolationsResponseDto;
import com.talex.server.dtos.responses.media.ViolationDetailResponseDto;
import com.talex.server.dtos.responses.media.CreatorViolationsSummaryDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.auth.Role;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.media.ContentCensorship;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaCopyright;
import com.talex.server.enums.media.CensorshipStatus;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.enums.media.MediaPlaybackPolicy;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.media.ContentPipelineService;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.MediaService;
import com.talex.server.services.media.MediaSystemConfigService;
import com.talex.server.services.series.EpisodeEntitlementService;
import com.talex.server.services.series.EpisodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final String URL_STORAGE_PROVIDER = "URL";
    private static final List<MediaStatus> PUBLIC_READY_STATUSES = List.of(MediaStatus.ACTIVE, MediaStatus.HLS_READY);
    private static final List<MediaStatus> VIDEO_REPLACEMENT_BLOCKING_STATUSES = List.of(
            MediaStatus.ACTIVE,
            MediaStatus.HLS_READY,
            MediaStatus.HLS_PROCESSING);

    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeService episodeService;
    private final MediaProviderService mediaProviderService;
    private final MediaPackagingService mediaPackagingService;
    private final MediaPlaybackSecurityService playbackSecurityService;
    private final ContentPipelineService contentPipelineService;
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final ContentCensorshipRepository contentCensorshipRepository;
    private final ContentOwnershipService contentOwnershipService;
    private final AccountRepository accountRepository;
    private final MediaSystemConfigService systemConfigService;
    private final CreatorRepository creatorRepository;
    private final EpisodeEntitlementService episodeEntitlementService;
    private final RestTemplate restTemplate;

    @Value("${python.api:http://localhost:8000}/watermark/embed")
    private String aiEmbedUrl;

    private record PreparedMediaUrl(
            String fileUrl,
            String checksum,
            String mimeType,
            Long fileSize,
            String storageProvider,
            MediaProvider provider) {
    }

    @Transactional
    @Override
    public MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        validateEpisodeStatusForMediaModification(episode);
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }
        MediaType mediaType = resolveMediaType(episode, request.getMediaType());
        return createOneFromUrl(episode, request, mediaType, request.getDisplayOrder());
    }

    @Transactional
    @Override
    public List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        validateEpisodeStatusForMediaModification(episode);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Batch media URL creation is only supported for comic episodes");
        }
        if (request == null || request.getPages() == null || request.getPages().isEmpty()) {
            throw ContentModuleException.badRequest("At least one comic page is required");
        }
        MediaSystemConfigResponseDto config = systemConfigService.getConfig();
        if (request.getPages().size() > config.getMaxComicImages()) {
            throw ContentModuleException.badRequest("Số lượng ảnh vượt quá giới hạn " + config.getMaxComicImages() + " ảnh");
        }

        List<Integer> resolvedOrders = resolveComicDisplayOrders(episode.getEpisodeId(), request.getPages());
        Set<String> requestedChecksums = new HashSet<>();
        List<MediaMetadataRequestDto> metadataRequests = new ArrayList<>();
        List<PreparedMediaUrl> preparedUrls = new ArrayList<>();

        for (MediaComicPageRequestDto page : request.getPages()) {
            MediaMetadataRequestDto metadata = new MediaMetadataRequestDto();
            metadata.setFileUrl(page.getFileUrl());
            metadata.setOriginalUrl(page.getFileUrl());
            metadata.setMimeType(page.getMimeType());
            metadata.setFileSize(page.getFileSize());
            metadata.setChecksum(page.getChecksum());
            metadata.setExternalPublicId(page.getExternalPublicId());
            metadata.setStorageProvider(page.getStorageProvider());
            metadata.setWidth(page.getWidth());
            metadata.setHeight(page.getHeight());
            metadata.setResolution(page.getResolution());
            metadata.setActorId(request.getActorId());
            PreparedMediaUrl preparedUrl = prepareUrl(metadata, MediaType.IMAGE);
            if (!requestedChecksums.add(preparedUrl.checksum())) {
                throw ContentModuleException.conflict("Duplicate media URL detected");
            }
            metadataRequests.add(metadata);
            preparedUrls.add(preparedUrl);
        }

        rejectDuplicateChecksums(requestedChecksums, null);

        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < metadataRequests.size(); i++) {
            Media media = new Media();
            media.setEpisode(episode);
            media.setCreatorId(episode.getCreatorId());
            media.setMediaType(MediaType.IMAGE);
            media.setDisplayOrder(resolvedOrders.get(i));
            applyPreparedUrl(media, metadataRequests.get(i), MediaType.IMAGE, preparedUrls.get(i));
            media.markCreatedBy(request.getActorId());
            mediaList.add(media);
        }

        List<Media> saved = mediaRepository.saveAll(mediaList);

        // Dispatch pipeline for each image — runs async via Kafka, doesn't block response
        for (Media media : saved) {
            try {
                contentPipelineService.dispatchPipelineJob(media);
            } catch (Exception e) {
                log.error("Failed to dispatch pipeline job for image mediaId={}", media.getMediaId(), e);
            }
        }

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getById(String id, String accountId) {
        Media media = findActiveEntity(id);
        contentOwnershipService.assertCanView(media, accountId);
        // Dùng chung endpoint này cho cả "Xem nội dung gốc" (SourceMediaPreviewModal, Staff/
        // Admin xem video source bị nghi trùng bản quyền) — xem lý do ký ở toAdminPreviewResponse().
        return toAdminPreviewResponse(media);
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getPublicById(String id, String viewerId) {
        Media media = findActiveEntity(id);
        if (!PUBLIC_READY_STATUSES.contains(media.getStatus())
                || media.getApprovalStatus() != ContentApprovalStatus.APPROVED) {
            throw ContentModuleException.notFound("Public media not found: " + id);
        }
        Episode episode = episodeService.findPublicEntity(media.getEpisode().getEpisodeId());
        if (episode.getStatus() == EpisodeStatus.SCHEDULED) {
            throw ContentModuleException.forbidden("Cannot access media for scheduled episode: " + episode.getEpisodeId());
        }
        
        boolean isEntitled = episodeEntitlementService.hasPlaybackAccess(viewerId, episode.getEpisodeId());
        MediaResponseDto response = toPublicResponse(media);
        
        if (!isEntitled) {
            if (episode.getContentType() == ContentType.COMIC) {
                // To be perfectly secure and consistent, we could check the index,
                // but since frontend doesn't use this API, we can just safely lock it.
                response.setIsLocked(true);
                response.setFileUrl(media.getPreviewUrl());
                response.setOriginalUrl(null);
                response.setPlaybackUrl(null);
                response.setHlsUrl(null);
                response.setSignedPlaybackUrl(null);
                response.setThumbnailUrl(null);
                response.setExternalPublicId(null);
                response.setProviderPublicId(null);
                response.setProviderAssetId(null);
                response.setDrmLicenseUrl(null);
                response.setDrmCertificateUrl(null);
                // Do not nullify previewUrl so frontend can use it if needed
            } else if (episode.getContentType() == ContentType.VIDEO) {
                throw ContentModuleException.forbidden("PLAYBACK_NOT_ENTITLED");
            }
        } else {
            response.setIsLocked(false);
        }

        return response;
    }

    @Override
    public ResponseEntity<byte[]> getWatermarkedImage(String mediaId, String viewerId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> ContentModuleException.notFound("Không tìm thấy media ID: " + mediaId));
        
        if (media.getMediaType() != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("Tính năng này chỉ hỗ trợ hình ảnh.");
        }
        
        String imageUrl = media.getFileUrl();
        byte[] originalBytes;
        try {
            originalBytes = restTemplate.getForObject(imageUrl, byte[].class);
            if (originalBytes == null) throw new RuntimeException("Tải ảnh gốc thất bại");
        } catch (Exception e) {
            log.error("Failed to download image from S3/CloudFront: {}", imageUrl, e);
            throw ContentModuleException.badRequest("Không thể tải ảnh gốc: " + e.getMessage());
        }

        String creatorId = media.getCreatorId();

        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            java.net.URL url = java.net.URI.create(aiEmbedUrl).toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(connection.getOutputStream())) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"media_type\"" + lineEnd + lineEnd);
                dos.writeBytes("IMAGE" + lineEnd);

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"creator_id\"" + lineEnd + lineEnd);
                dos.writeBytes((creatorId != null ? creatorId : "") + lineEnd);
                
                if (viewerId != null && !viewerId.isBlank()) {
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"viewer_id\"" + lineEnd + lineEnd);
                    dos.writeBytes(viewerId + lineEnd);
                }

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.png\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);
                dos.write(originalBytes);
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
            }

            int serverResponseCode = connection.getResponseCode();
            if (serverResponseCode >= 200 && serverResponseCode < 300) {
                try (java.io.InputStream is = connection.getInputStream()) {
                    byte[] watermarkedBytes = is.readAllBytes();
                    return ResponseEntity.ok()
                            .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                            .body(watermarkedBytes);
                }
            } else {
                throw new RuntimeException("AI API returned " + serverResponseCode);
            }
        } catch (Exception e) {
            log.error("Failed to embed watermark via AI", e);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                    .body(originalBytes);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<MediaResponseDto> listByEpisode(String episodeId, String accountId) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        contentOwnershipService.assertCanView(episode, accountId);
        return mediaRepository.findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<MediaResponseDto> listPublicByEpisode(String episodeId, String viewerId) {
        Episode episode = episodeService.findPublicEntity(episodeId);
        if (episode.getStatus() == EpisodeStatus.SCHEDULED) {
            throw ContentModuleException.forbidden("Cannot access media for scheduled episode: " + episodeId);
        }
        boolean isEntitled = episodeEntitlementService.hasPlaybackAccess(viewerId, episodeId);
        
        List<MediaResponseDto> mediaList = mediaRepository
                .findAllByEpisode_EpisodeIdAndStatusInAndApprovalStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
                        episodeId,
                        PUBLIC_READY_STATUSES,
                        ContentApprovalStatus.APPROVED)
                .stream()
                .map(this::toPublicResponse)
                .sorted(Comparator.comparing(MediaResponseDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(java.util.stream.Collectors.toList());

        if (!isEntitled) {
            if (episode.getContentType() == ContentType.COMIC) {
                for (int i = 0; i < mediaList.size(); i++) {
                    MediaResponseDto media = mediaList.get(i);
                    if (i < 5) {
                        media.setIsLocked(false);
                    } else {
                        media.setIsLocked(true);
                        media.setFileUrl(media.getPreviewUrl());
                        media.setOriginalUrl(null);
                        media.setPlaybackUrl(null);
                        media.setHlsUrl(null);
                        media.setSignedPlaybackUrl(null);
                        media.setThumbnailUrl(null);
                        media.setExternalPublicId(null);
                        media.setProviderPublicId(null);
                        media.setProviderAssetId(null);
                        media.setDrmLicenseUrl(null);
                        media.setDrmCertificateUrl(null);
                        // Do not nullify previewUrl so frontend can use it if needed
                    }
                }
            } else if (episode.getContentType() == ContentType.VIDEO) {
                throw ContentModuleException.forbidden("PLAYBACK_NOT_ENTITLED");
            }
        } else {
            for (MediaResponseDto media : mediaList) {
                media.setIsLocked(false);
            }
        }

        return mediaList;
    }

    @Transactional
    @Override
    public MediaResponseDto update(String id, MediaUpdateRequestDto request, String accountId) {
        Media media = findManageableEntity(id, accountId);
        if (request.getWidth() != null) {
            media.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            media.setHeight(request.getHeight());
        }
        if (request.getResolution() != null) {
            media.setResolution(request.getResolution());
        }
        if (request.getDuration() != null) {
            media.setDuration(request.getDuration());
        }
        if (request.getDisplayOrder() != null) {
            ensureImageMedia(media);
            Episode episode = lockActiveEpisode(media.getEpisode().getEpisodeId());
            ensureDisplayOrderAvailable(
                    episode.getEpisodeId(),
                    request.getDisplayOrder(),
                    media.getMediaId());
            media.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            media.setStatus(request.getStatus());
        }
        media.markUpdatedBy(accountId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request, String accountId) {
        Media media = findManageableEntity(id, accountId);
        Episode episode = lockActiveEpisode(media.getEpisode().getEpisodeId());
        MediaType mediaType = resolveMediaType(
                episode,
                request != null && request.getMediaType() != null ? request.getMediaType() : media.getMediaType());
        if (media.getMediaType() != mediaType) {
            throw ContentModuleException.badRequest("Replacement URL type must match existing media type");
        }
        validateMediaForEpisode(episode, mediaType, media.getMediaId());
        applyUrl(media, request, mediaType);
        media.markUpdatedBy(accountId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Only comic episode media can be reordered");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw ContentModuleException.badRequest("At least one media reorder item is required");
        }
        if (request.getItems().stream().anyMatch(Objects::isNull)) {
            throw ContentModuleException.badRequest("Media reorder item must not be null");
        }

        Set<String> mediaIds = new LinkedHashSet<>();
        Set<Integer> displayOrders = new LinkedHashSet<>();
        for (var item : request.getItems()) {
            if (item.getMediaId() == null || item.getMediaId().isBlank()) {
                throw ContentModuleException.badRequest("mediaId is required");
            }
            if (!mediaIds.add(item.getMediaId().trim())) {
                throw ContentModuleException.badRequest("mediaId must not contain duplicates");
            }
            if (item.getDisplayOrder() == null || item.getDisplayOrder() < 1) {
                throw ContentModuleException.badRequest("displayOrder must be positive numbers");
            }
            if (!displayOrders.add(item.getDisplayOrder())) {
                throw ContentModuleException.badRequest("displayOrder must not contain duplicates");
            }
        }

        Map<String, Media> mediaById = mediaRepository.findAllByMediaIdInAndIsDeletedFalse(mediaIds)
                .stream()
                .collect(Collectors.toMap(Media::getMediaId, Function.identity()));
        if (mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndMediaIdNotInAndIsDeletedFalse(
                episodeId,
                displayOrders,
                mediaIds)) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }

        List<Media> changedMedia = new ArrayList<>();
        for (var item : request.getItems()) {
            Media media = mediaById.get(item.getMediaId().trim());
            if (media == null) {
                throw ContentModuleException.notFound("Media not found: " + item.getMediaId());
            }
            if (!media.getEpisode().getEpisodeId().equals(episodeId)) {
                throw ContentModuleException.badRequest("Media does not belong to episode: " + item.getMediaId());
            }
            ensureImageMedia(media);
            media.setDisplayOrder(item.getDisplayOrder());
            media.markUpdatedBy(accountId);
            changedMedia.add(media);
        }
        mediaRepository.saveAll(changedMedia);

        return listByEpisode(episodeId, accountId);
    }

    @Transactional
    @Override
    public MediaResponseDto hide(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        media.setStatus(MediaStatus.HIDDEN);
        media.markUpdatedBy(actorId);
        playbackSecurityService.revokeActiveSessions(media);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto unhide(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        contentOwnershipService.assertCanManage(media, actorId);
        if (media.getStatus() != MediaStatus.HIDDEN) {
            throw ContentModuleException.badRequest("Chỉ có thể bỏ ẩn khi media đang ở trạng thái HIDDEN");
        }
        media.setStatus(MediaStatus.ACTIVE);
        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    @Transactional
    @Override
    public MediaResponseDto forceHide(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setStatus(MediaStatus.FORCE_HIDDEN);
        // Thiếu dòng này trước đây khiến viewer đang có signed URL/session còn hiệu lực
        // (SIGNED_PLAYBACK_TTL_SECONDS) vẫn xem tiếp được tới khi tự hết hạn, dù admin đã
        // ép ẩn — mọi hàm gỡ nội dung khác (hide/reject/rejectWithReason/delete) đều gọi
        // hàm này, chỉ forceHide() bị sót.
        playbackSecurityService.revokeActiveSessions(media);
        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    @Transactional
    @Override
    public MediaResponseDto forceUnhide(String id, String actorId) {
        Media media = findActiveEntity(id);
        if (media.getStatus() != MediaStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Media is not force-hidden");
        }
        media.setStatus(MediaStatus.HIDDEN);
        Media saved = mediaRepository.save(media);
        return toResponse(saved);
    }

    @Transactional
    @Override
    public MediaResponseDto approve(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.APPROVED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        // Trước đây chỉ đổi approvalStatus — nếu media đang INACTIVE (bị pipeline flag
        // chờ Staff duyệt, xem ContentPipelineServiceImpl), Staff duyệt xong media vẫn
        // kẹt INACTIVE vĩnh viễn, không ai xem lại được (kể cả Creator/viewer công khai).
        if (media.getStatus() == MediaStatus.INACTIVE) {
            // VIDEO: status bị ContentPipelineServiceImpl ghi đè thành INACTIVE KHÔNG
            // ĐIỀU KIỆN khi bị flag chờ Staff — kể cả khi transcode HLS còn đang chạy dở
            // (kiểm duyệt và transcode chạy song song, kiểm duyệt thường xong trước).
            // Dùng hlsReadyAt (field riêng, chỉ SqsMediaEventPoller.markHlsReady() ghi,
            // không bao giờ bị luồng kiểm duyệt đụng vào) để biết CHẮC CHẮN transcode đã
            // xong chưa — không suy đoán qua status như trước (dễ sai cả 2 chiều: chuyển
            // ACTIVE sớm khi video thật ra chưa phát được, hoặc kẹt mãi nếu suy đoán
            // ngược lại). Ảnh không có bước transcode nên luôn coi như sẵn sàng.
            boolean hlsAlreadyReady = media.getMediaType() != MediaType.VIDEO || media.getHlsReadyAt() != null;
            if (hlsAlreadyReady) {
                media.setStatus(MediaStatus.ACTIVE);
            }
            // Video chưa xong transcode: giữ nguyên INACTIVE — approvalStatus đã APPROVED
            // ở trên, SqsMediaEventPoller.markHlsReady() sẽ tự chuyển ACTIVE khi transcode
            // xong thật (đã có check approvalStatus==APPROVED từ trước).
        }
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto reject(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.REJECTED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        if (media.getStatus() == MediaStatus.ACTIVE || media.getStatus() == MediaStatus.HLS_READY) {
            media.setStatus(MediaStatus.HIDDEN);
            playbackSecurityService.revokeActiveSessions(media);
        }
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    @Override
    public CreatorViolationsSummaryDto getCreatorViolationsSummary(String creatorId) {
        long copyrightStrikes = mediaCopyrightRepository.countByMedia_CreatorIdAndIsValidFalse(creatorId);
        long censorshipStrikes = contentCensorshipRepository.countByMedia_CreatorIdAndStatus(creatorId, com.talex.server.enums.media.CensorshipStatus.REJECTED);
        
        return CreatorViolationsSummaryDto.builder()
                .creatorId(creatorId)
                .totalCopyrightStrikes(copyrightStrikes)
                .totalCensorshipStrikes(censorshipStrikes)
                .build();
    }

    @Transactional
    @Override
    public MediaResponseDto rejectWithReason(String id, String actorId, MediaRejectRequestDto request) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.REJECTED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        if (media.getStatus() == MediaStatus.ACTIVE || media.getStatus() == MediaStatus.HLS_READY) {
            media.setStatus(MediaStatus.HIDDEN);
            playbackSecurityService.revokeActiveSessions(media);
        }
        media.markUpdatedBy(actorId);
        mediaRepository.save(media);

        // Record human review decision as a censorship entry with staff's reason
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "";
        ContentCensorship censorship = new ContentCensorship();
        censorship.setMedia(media);
        censorship.setReviewedBy("HUMAN");
        censorship.setReviewerNotes(reason);
        censorship.setStatus(CensorshipStatus.REJECTED);
        censorship.setCheckedAt(LocalDateTime.now());
        contentCensorshipRepository.save(censorship);

        // Báo real-time cho creator — trước đây creator phải tự load lại trang mới thấy
        // được lý do Staff từ chối, vì luồng duyệt tay này không bắn SSE như luồng AI
        // tự động flag lúc upload.
        contentPipelineService.notifyStaffRejected(media, reason);

        return toResponse(media);
    }

    @Transactional
    @Override
    public MediaResponseDto retryPipeline(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        if (media.getStatus() != MediaStatus.FAILED) {
            throw ContentModuleException.badRequest("Chỉ có thể thử lại khi media đang ở trạng thái FAILED");
        }

        // Dọn sạch kết quả cũ — nếu không, idempotency check trong
        // ContentPipelineServiceImpl.handleCopyrightResult()/handleModerationResult() sẽ
        // tự skip toàn bộ (coi như đã xử lý rồi), retry sẽ không có tác dụng gì.
        media.setContentId(null);
        media.setErrorMessage(null);
        mediaCopyrightRepository.deleteAll(mediaCopyrightRepository.findAllByMedia_MediaId(media.getMediaId()));
        contentCensorshipRepository.deleteAll(contentCensorshipRepository.findAllByMedia_MediaId(media.getMediaId()));

        if (media.getMediaType() == MediaType.VIDEO) {
            // Resubmit transcode trên file gốc đã có sẵn ở S3/Cloudinary — không cần upload
            // lại. Tự set HLS_PROCESSING, route đúng provider theo media.getProvider().
            mediaPackagingService.createHlsPackaging(media);
        }
        media.markUpdatedBy(actorId);
        media = mediaRepository.save(media);

        contentPipelineService.dispatchPipelineJob(media);
        return toResponse(media);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        log.warn("Media delete requested: mediaId={}, actorId={}, episodeId={}",
                id, actorId, media.getEpisode() != null ? media.getEpisode().getEpisodeId() : null);
        validateEpisodeStatusForMediaModification(media.getEpisode());
        media.setStatus(MediaStatus.DELETED);
        media.softDelete(actorId);
        playbackSecurityService.revokeActiveSessions(media);
        if (media.getMediaType() == MediaType.VIDEO && media.getProviderPublicId() != null) {
            mediaProviderService.deleteAsset(media);
        }
        contentPipelineService.notifyMediaDeleted(media.getMediaId());
        // Dọn bản ghi review/vi phạm còn treo cho media này — nếu không, ContentCensorship/
        // MediaCopyright PENDING mồ côi vẫn nằm trong DB trỏ tới media đã xóa. Hiện tại được
        // "bảo vệ nhờ may mắn" vì hàng đợi Staff lọc theo Media.status nên không hiển thị ra,
        // nhưng approve()/reject() qua findActiveEntity() sẽ báo 404 nếu ai đó cố duyệt bản ghi
        // này từ 1 tab cũ đã mở sẵn trước khi bị xóa — dọn hẳn để tránh rác dữ liệu vĩnh viễn.
        mediaCopyrightRepository.deleteAll(mediaCopyrightRepository.findAllByMedia_MediaId(media.getMediaId()));
        contentCensorshipRepository.deleteAll(contentCensorshipRepository.findAllByMedia_MediaId(media.getMediaId()));
        mediaRepository.save(media);
    }

    @Override
    public Media findActiveEntity(String id) {
        return mediaRepository.findByMediaIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + id));
    }

    @Override
    public Media findManageableEntity(String id, String accountId) {
        Media media = mediaRepository
                .findByMediaIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + id));
        contentOwnershipService.assertCanManage(media, accountId);
        return media;
    }

    @Transactional(readOnly = true)
    @Override
    public MediaViolationsResponseDto getMediaViolations(String mediaId, String accountId) {
        Media media = findActiveEntity(mediaId);
        // Trước đây endpoint này KHÔNG kiểm tra quyền — bất kỳ user đăng nhập nào cũng xem
        // được violation của media người khác (IDOR). assertCanView cho phép chủ sở hữu
        // hoặc STAFF/ADMIN, khớp đúng pattern getById() đã làm.
        contentOwnershipService.assertCanView(media, accountId);
        boolean privileged = contentOwnershipService.isPrivileged();

        List<MediaCopyright> copyrightEntities = mediaCopyrightRepository.findAllByMedia_MediaId(mediaId);
        List<ContentCensorship> censorshipEntities = contentCensorshipRepository.findAllByMedia_MediaId(mediaId);

        List<MediaCopyrightResponseDto> copyrightDtos = copyrightEntities.stream()
                .map(entity -> mapCopyrightToDto(entity, privileged))
                .toList();

        List<ContentCensorshipResponseDto> censorshipDtos = censorshipEntities.stream()
                .map(this::mapCensorshipToDto)
                .toList();

        return MediaViolationsResponseDto.builder()
                .mediaId(media.getMediaId())
                // Trước đây trả nhầm providerPublicId (S3 object key) — không phải Content
                // ID thật (do AI service gán, dạng "CID-{mediaId}", lưu ở Media.contentId).
                .contentId(media.getContentId())
                .copyrightViolations(copyrightDtos)
                .censorshipResults(censorshipDtos)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MediaResponseDto> listPendingReview(int page, int size, String mediaType, String keyword) {
        // KHÔNG group theo episode ở tab này — khác với listApproved() (force-hide/unhide
        // tác động cả episode nên group được), Duyệt/Từ chối ở tab "Chờ duyệt" tác động lên
        // TỪNG Media riêng lẻ (xem MediaController.approve/reject dùng media id). Nếu group,
        // Staff sẽ không thấy được các media khác cùng episode cần duyệt riêng — mất nội
        // dung cần kiểm duyệt, không chỉ là trùng lặp hiển thị.
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        MediaType typeFilter = parseMediaTypeFilter(mediaType);
        // PENDING_REVIEW cũng là giá trị MẶC ĐỊNH của approvalStatus trước khi pipeline
        // từng chạy xong (xem Media.java) — nếu chỉ lọc theo approvalStatus, hàng đợi
        // Staff sẽ lẫn cả media đang xử lý dở (chưa có kết quả gì) với media THẬT SỰ bị
        // flag cần duyệt. BE chỉ set MediaStatus.INACTIVE khi chủ động flag nội dung
        // (xem ContentPipelineServiceImpl) — thêm điều kiện này để lọc đúng.
        return mediaRepository
                .findPendingReviewMedia(ContentApprovalStatus.PENDING_REVIEW, MediaStatus.INACTIVE,
                        parseKeyword(keyword), typeFilter, pageable)
                .map(this::toAdminPreviewResponse);
    }

    // Phân trang theo EPISODE (không phải theo Media riêng lẻ) — 1 episode có thể có hàng
    // chục Media (VD comic nhiều trang), phân trang thô theo Media sẽ cắt 1 episode rải ra
    // nhiều trang, FE thấy episode đó "xuất hiện lại" ở trang sau với số lượng khác (bug
    // thật đã gặp). Media đại diện = displayOrder nhỏ nhất (trang đầu/thumbnail của episode).
    private Page<MediaResponseDto> groupByEpisode(Page<String> episodeIdsPage, List<Media> media) {
        Map<String, List<Media>> byEpisode = media.stream()
                .collect(Collectors.groupingBy(m -> m.getEpisode().getEpisodeId()));
        List<MediaResponseDto> content = episodeIdsPage.getContent().stream()
                .map(byEpisode::get)
                .filter(group -> group != null && !group.isEmpty())
                .map(group -> {
                    Media representative = group.stream()
                            .min(Comparator.comparing(
                                    Media::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(group.get(0));
                    MediaResponseDto dto = toAdminPreviewResponse(representative);
                    dto.setEpisodeMediaCount(group.size());
                    return dto;
                })
                .toList();
        return new PageImpl<>(content, episodeIdsPage.getPageable(), episodeIdsPage.getTotalElements());
    }

    // Chuẩn hóa keyword giống AccountSpecification (AdminAccountServiceImpl) — trim, lowercase,
    // bọc "%...%" cho LIKE — null/rỗng thành null để query JPQL hiểu là "không lọc" (:keyword IS NULL).
    private String parseKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    // "ALL"/null/giá trị không hợp lệ đều coi là không lọc — tránh 400 lặt vặt cho FE khi
    // filter đang ở trạng thái mặc định "Tất cả".
    private MediaType parseMediaTypeFilter(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        try {
            return MediaType.valueOf(mediaType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // CloudFront distribution chứa source video (source/videos/...) bắt buộc signed URL
    // (trusted key group) — originalUrl trong toResponse() luôn là URL trần, Staff/Admin
    // mở "Chi tiết kiểm duyệt" bị 403 MissingKey không xem được video gốc. Chỉ ký riêng
    // cho 2 endpoint admin duyệt nội dung (không đổi toResponse() dùng chung ở nơi khác,
    // tránh ảnh hưởng ngoài ý muốn tới các luồng đã hoạt động đúng).
    private MediaResponseDto toAdminPreviewResponse(Media media) {
        MediaResponseDto dto = toResponse(media);
        if (media.getMediaType() == MediaType.VIDEO && dto.getOriginalUrl() != null) {
            dto.setOriginalUrl(
                    mediaProviderService.signSingleUrl(dto.getOriginalUrl(), LocalDateTime.now().plusHours(1)));
        }
        enrichEpisodeContext(dto, media);
        return dto;
    }

    // Staff/Admin duyệt cần biết nội dung thuộc episode/season/series/creator nào — episodeId
    // thô không đủ để ra quyết định nhanh. Dùng chung cho cả tab "Chờ duyệt" và "Đã duyệt"
    // (xem toAdminPreviewResponse).
    private void enrichEpisodeContext(MediaResponseDto dto, Media media) {
        Episode episode = media.getEpisode();
        if (episode == null) {
            return;
        }
        dto.setEpisodeTitle(episode.getTitle());
        Season season = episode.getSeason();
        if (season != null) {
            dto.setSeasonTitle(season.getTitle());
            if (season.getSeries() != null) {
                dto.setSeriesTitle(season.getSeries().getTitle());
            }
        }
        String creatorId = media.getCreatorId();
        if (creatorId != null && !creatorId.isBlank()) {
            dto.setCreatorUsername(
                    creatorRepository.findById(creatorId)
                            .map(Creator::getAccount)
                            .map(Account::getUsername)
                            .orElse("Tài khoản không xác định"));
        }
    }

    private static final List<MediaStatus> APPROVED_TAB_STATUSES =
            List.of(MediaStatus.ACTIVE, MediaStatus.HLS_READY, MediaStatus.FORCE_HIDDEN);

    // Phải khớp CHÍNH XÁC với ContentPipelineServiceImpl.PIPELINE_ACTOR — dùng để phân biệt
    // "pipeline tự duyệt" (approvalReviewedBy = giá trị này) với "Staff/Admin tự tay duyệt"
    // (approvalReviewedBy = accountId thật) ở filter "manual"/"clean" bên dưới.
    private static final String PIPELINE_REVIEWER_ACTOR = "content-pipeline";

    // approvalReviewedBy chỉ lưu accountId thô (hoặc PIPELINE_REVIEWER_ACTOR) — FE cần tên
    // + vai trò để hiển thị "ai đã duyệt" thay vì UUID khó đọc. accountId ở đây LUÔN là
    // Account.accountId thật (không phải Creator id) vì chỉ Staff/Admin mới gọi được
    // approve()/rejectWithReason() (xem @PreAuthorize ở MediaController), khác với
    // sourceCreatorId ở resolveCopyrightSource() cần đi qua Creator -> Account.
    private Optional<Account> resolveReviewerAccount(String reviewedBy) {
        if (reviewedBy == null || reviewedBy.isBlank() || reviewedBy.equals(PIPELINE_REVIEWER_ACTOR)) {
            return Optional.empty();
        }
        try {
            return accountRepository.findById(UUID.fromString(reviewedBy));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String resolveReviewerName(String reviewedBy) {
        if (PIPELINE_REVIEWER_ACTOR.equals(reviewedBy)) {
            return "Hệ thống (tự động)";
        }
        return resolveReviewerAccount(reviewedBy)
                .map(account -> account.getFullName() != null && !account.getFullName().isBlank()
                        ? account.getFullName()
                        : account.getUsername())
                .orElse(null);
    }

    private String resolveReviewerRole(String reviewedBy) {
        return resolveReviewerAccount(reviewedBy)
                .map(Account::getRole)
                .map(Role::getCode)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MediaResponseDto> listApproved(int page, int size, String reviewFilter, String mediaType,
            String keyword) {
        // Sắp theo approvalReviewedAt (lượt duyệt gần nhất lên đầu, xem ORDER BY trong query
        // ở repository) — mục đích tab này là Staff/Admin rà lại các quyết định VỪA duyệt để
        // phát hiện lỡ bấm nhầm, không phải duyệt toàn bộ lịch sử theo thời gian tạo.
        PageRequest pageable = PageRequest.of(page, size);
        MediaType typeFilter = parseMediaTypeFilter(mediaType);
        String normalizedKeyword = parseKeyword(keyword);
        Page<String> episodeIdsPage;
        List<Media> media;
        if ("manual".equals(reviewFilter)) {
            episodeIdsPage = mediaRepository.findDistinctManuallyApprovedEpisodeIds(
                    ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES, PIPELINE_REVIEWER_ACTOR,
                    typeFilter, normalizedKeyword, pageable);
            media = mediaRepository.findManuallyApprovedMediaByEpisodeIds(
                    episodeIdsPage.getContent(), ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES,
                    PIPELINE_REVIEWER_ACTOR, typeFilter);
        } else if ("clean".equals(reviewFilter)) {
            episodeIdsPage = mediaRepository.findDistinctAutoApprovedEpisodeIds(
                    ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES, PIPELINE_REVIEWER_ACTOR,
                    typeFilter, normalizedKeyword, pageable);
            media = mediaRepository.findAutoApprovedMediaByEpisodeIds(
                    episodeIdsPage.getContent(), ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES,
                    PIPELINE_REVIEWER_ACTOR, typeFilter);
        } else {
            episodeIdsPage = mediaRepository.findDistinctApprovedEpisodeIds(
                    ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES, typeFilter, normalizedKeyword, pageable);
            media = mediaRepository.findApprovedMediaByEpisodeIds(
                    episodeIdsPage.getContent(), ContentApprovalStatus.APPROVED, APPROVED_TAB_STATUSES, typeFilter);
        }
        return groupByEpisode(episodeIdsPage, media);
    }

    @Override
    public MediaResponseDto toResponse(Media media) {
        return baseResponse(media).build();
    }

    private void validateEpisodeStatusForMediaModification(Episode episode) {
        if (episode.getStatus() != EpisodeStatus.DRAFT && episode.getStatus() != EpisodeStatus.HIDDEN) {
            throw ContentModuleException.badRequest("Cannot modify media when episode status is " + episode.getStatus());
        }
    }

    private MediaResponseDto toPublicResponse(Media media) {
        MediaResponseDto.MediaResponseDtoBuilder builder = baseResponse(media);
        boolean protectedVideo = media.getMediaType() == MediaType.VIDEO
                && media.getProtectionType() != MediaProtectionType.NONE
                && media.getPlaybackPolicy() != MediaPlaybackPolicy.PUBLIC;
        if (protectedVideo) {
            builder.fileUrl(null)
                    .originalUrl(null)
                    .hlsUrl(null)
                    .playbackUrl(null)
                    .signedPlaybackUrl(null);
        }
        return builder.build();
    }

    private MediaResponseDto.MediaResponseDtoBuilder baseResponse(Media media) {
        return MediaResponseDto.builder()
                .mediaId(media.getMediaId())
                .episodeId(media.getEpisode().getEpisodeId())
                .episodeStatus(media.getEpisode().getStatus())
                .creatorId(media.getCreatorId())
                .mediaType(media.getMediaType())
                .mimeType(media.getMimeType())
                .fileUrl(media.getFileUrl())
                .externalPublicId(media.getExternalPublicId())
                .storageProvider(media.getStorageProvider())
                .provider(media.getProvider())
                .providerAssetId(media.getProviderAssetId())
                .providerPublicId(media.getProviderPublicId())
                .providerDeliveryType(media.getProviderDeliveryType())
                .originalUrl(media.getOriginalUrl())
                .playbackUrl(media.getPlaybackUrl())
                .hlsUrl(media.getHlsUrl())
                .signedPlaybackUrl(media.getSignedPlaybackUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .previewUrl(media.getPreviewUrl())
                .format(media.getFormat())
                .protectionType(media.getProtectionType())
                .playbackPolicy(media.getPlaybackPolicy())
                .drmProvider(media.getDrmProvider())
                .drmKeyId(media.getDrmKeyId())
                .drmLicenseUrl(media.getDrmLicenseUrl())
                .drmCertificateUrl(media.getDrmCertificateUrl())
                .tokenTtlSeconds(media.getTokenTtlSeconds())
                .errorMessage(media.getErrorMessage())
                .pendingDelete(media.getPendingDelete())
                .fileSize(media.getFileSize())
                .checksum(media.getChecksum())
                .width(media.getWidth())
                .height(media.getHeight())
                .resolution(media.getResolution())
                .duration(media.getDuration())
                .displayOrder(media.getDisplayOrder())
                .status(media.getStatus())
                .approvalStatus(media.getApprovalStatus())
                .approvalReviewedAt(media.getApprovalReviewedAt())
                .approvalReviewedBy(media.getApprovalReviewedBy())
                .approvalReviewedByName(resolveReviewerName(media.getApprovalReviewedBy()))
                .approvalReviewedByRole(resolveReviewerRole(media.getApprovalReviewedBy()))
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .deletedAt(media.getDeletedAt())
                .createdBy(media.getCreatedBy())
                .updatedBy(media.getUpdatedBy())
                .deletedBy(media.getDeletedBy())
                .isDeleted(media.getIsDeleted())
                .contentId(media.getContentId());
    }

    private MediaCopyrightResponseDto mapCopyrightToDto(MediaCopyright entity, boolean privileged) {
        MediaCopyrightResponseDto.MediaCopyrightResponseDtoBuilder builder = MediaCopyrightResponseDto.builder()
                .mediaCopyrightId(entity.getMediaCopyrightId())
                .mediaId(entity.getMedia().getMediaId())
                .sourceMediaId(entity.getSourceMedia() != null ? entity.getSourceMedia().getMediaId() : null)
                .startTimeTarget(entity.getStartTimeTarget())
                .endTimeTarget(entity.getEndTimeTarget())
                .startTimeSource(entity.getStartTimeSource())
                .endTimeSource(entity.getEndTimeSource())
                .similarityScore(entity.getSimilarityScore())
                .violationType(entity.getViolationType())
                .isValid(entity.getIsValid())
                .note(entity.getNote())
                .checkedAt(entity.getCheckedAt());

        // Thông tin nội dung gốc bị trùng (tên tập/series, creator, thumbnail) chỉ trả cho
        // STAFF/ADMIN — Creator tự xem violation của mình KHÔNG được biết đã trùng với ai
        // (tránh lộ danh tính creator khác, nguy cơ harassment hoặc "học lỏm" né kiểm duyệt).
        if (privileged) {
            enrichSourceContent(builder, entity.getSourceMedia());
        }

        return builder.build();
    }

    private void enrichSourceContent(MediaCopyrightResponseDto.MediaCopyrightResponseDtoBuilder builder, Media sourceMedia) {
        if (sourceMedia == null) {
            return;
        }
        builder.sourceMediaDeleted(Boolean.TRUE.equals(sourceMedia.getIsDeleted()));
        builder.sourceThumbnailUrl(sourceMedia.getThumbnailUrl());

        Episode sourceEpisode = sourceMedia.getEpisode();
        if (sourceEpisode != null) {
            builder.sourceEpisodeTitle(sourceEpisode.getTitle());
            if (sourceEpisode.getSeason() != null && sourceEpisode.getSeason().getSeries() != null) {
                builder.sourceSeriesTitle(sourceEpisode.getSeason().getSeries().getTitle());
            }
        }

        String sourceCreatorId = sourceMedia.getCreatorId();
        if (sourceCreatorId == null || sourceCreatorId.isBlank()) {
            return;
        }
        // sourceCreatorId là Creator entity id (media.getCreatorId()), KHÔNG phải accountId
        // — trước đây tra thẳng vào AccountRepository bằng ID này nên luôn miss (2 bảng
        // khác nhau), hiển thị nhầm "Tài khoản không xác định" cho mọi trường hợp. Phải đi
        // qua Creator -> Account mới đúng, giống pattern đã áp dụng ở EpisodeServiceImpl
        // khi resolve accountId để gửi notification.
        builder.sourceCreatorUsername(
                creatorRepository.findById(sourceCreatorId)
                        .map(Creator::getAccount)
                        .map(Account::getUsername)
                        .orElse("Tài khoản không xác định"));
    }

    private ContentCensorshipResponseDto mapCensorshipToDto(ContentCensorship entity) {
        List<ViolationDetailResponseDto> details = entity.getViolationDetails() == null
                ? List.of()
                : entity.getViolationDetails().stream()
                        .map(vd -> ViolationDetailResponseDto.builder()
                                .violationDetailId(vd.getViolationDetailId())
                                .violationAt(vd.getViolationAt())
                                .endViolationAt(vd.getEndViolationAt())
                                .label(vd.getLabel())
                                .confidence(vd.getConfidence())
                                .suggestion(vd.getSuggestion())
                                .build())
                        .toList();

        return ContentCensorshipResponseDto.builder()
                .censorshipId(entity.getCensorshipId())
                .mediaId(entity.getMedia().getMediaId())
                .primaryViolationLabel(entity.getPrimaryViolationLabel())
                .confidenceScore(entity.getConfidenceScore())
                .checkedAt(entity.getCheckedAt())
                .reviewedBy(entity.getReviewedBy())
                .reviewerNotes(entity.getReviewerNotes())
                .status(entity.getStatus())
                .violationDetails(details)
                .build();
    }

    private MediaResponseDto createOneFromUrl(
            Episode episode,
            MediaMetadataRequestDto request,
            MediaType mediaType,
            Integer requestedDisplayOrder) {

        validateMediaForEpisode(episode, mediaType, null);

        Media media = new Media();
        media.setEpisode(episode);
        media.setCreatorId(episode.getCreatorId());
        media.setMediaType(mediaType);
        media.setDisplayOrder(resolveDisplayOrder(episode.getEpisodeId(), mediaType, requestedDisplayOrder));
        applyUrl(media, request, mediaType);
        media.markCreatedBy(request.getActorId());

        Media saved = mediaRepository.save(media);

        // Dispatch pipeline for IMAGE media — runs async via Kafka, doesn't block response
        if (mediaType == MediaType.IMAGE) {
            try {
                contentPipelineService.dispatchPipelineJob(saved);
            } catch (Exception e) {
                log.error("Failed to dispatch pipeline job for image mediaId={}", saved.getMediaId(), e);
            }
        }

        return toResponse(saved);
    }

    private void applyUrl(Media media, MediaMetadataRequestDto request, MediaType mediaType) {
        PreparedMediaUrl preparedUrl = prepareUrl(request, mediaType);
        rejectDuplicate(preparedUrl.checksum(), media.getMediaId());
        applyPreparedUrl(media, request, mediaType, preparedUrl);
    }

    private PreparedMediaUrl prepareUrl(MediaMetadataRequestDto request, MediaType mediaType) {
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }

        String fileUrl = normalizeFileUrl(request.getFileUrl());
        String checksum = normalizeChecksum(request.getChecksum(), fileUrl);
        String mimeType = validateMimeType(request.getMimeType(), mediaType);
        Long fileSize = validateFileSize(request.getFileSize());
        String storageProvider = normalizeStorageProvider(request.getStorageProvider());
        MediaProvider provider = request.getProvider() != null
                ? request.getProvider()
                : resolveProvider(request.getStorageProvider());
        return new PreparedMediaUrl(fileUrl, checksum, mimeType, fileSize, storageProvider, provider);
    }

    private void applyPreparedUrl(
            Media media,
            MediaMetadataRequestDto request,
            MediaType mediaType,
            PreparedMediaUrl preparedUrl) {
        media.setFileUrl(preparedUrl.fileUrl());
        media.setOriginalUrl(firstNonBlank(request.getOriginalUrl(), preparedUrl.fileUrl()));
        media.setPlaybackUrl(firstNonBlank(request.getPlaybackUrl(), request.getHlsUrl(), preparedUrl.fileUrl()));
        media.setHlsUrl(request.getHlsUrl());
        media.setThumbnailUrl(request.getThumbnailUrl());
        media.setPreviewUrl(request.getPreviewUrl());
        media.setChecksum(preparedUrl.checksum());
        media.setMimeType(preparedUrl.mimeType());
        media.setFileSize(preparedUrl.fileSize());
        media.setExternalPublicId(blankToNull(request.getExternalPublicId()));
        media.setStorageProvider(preparedUrl.storageProvider());
        media.setProvider(preparedUrl.provider());
        media.setProviderAssetId(blankToNull(request.getProviderAssetId()));
        media.setProviderPublicId(firstNonBlank(request.getProviderPublicId(), request.getExternalPublicId()));
        media.setProviderDeliveryType(blankToNull(request.getProviderDeliveryType()));
        media.setFormat(blankToNull(request.getFormat()));
        media.setProtectionType(request.getProtectionType() != null ? request.getProtectionType() : MediaProtectionType.NONE);
        media.setPlaybackPolicy(request.getPlaybackPolicy() != null ? request.getPlaybackPolicy() : MediaPlaybackPolicy.PUBLIC);
        media.setStatus(MediaStatus.ACTIVE);
        media.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
        media.setApprovalReviewedAt(null);
        media.setApprovalReviewedBy(null);
        applyMetadata(media, request, mediaType);
    }

    private void applyMetadata(Media media, MediaMetadataRequestDto request, MediaType mediaType) {
        if (request.getWidth() != null) {
            media.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            media.setHeight(request.getHeight());
        }
        if (request.getResolution() != null) {
            media.setResolution(request.getResolution());
        }
        if (request.getDuration() != null) {
            media.setDuration(request.getDuration());
        }
        if (mediaType == MediaType.IMAGE && request.getDisplayOrder() != null) {
            media.setDisplayOrder(request.getDisplayOrder());
        }
    }

    private MediaType resolveMediaType(Episode episode, MediaType requestedType) {
        MediaType mediaType = requestedType;
        if (mediaType == null) {
            mediaType = episode.getContentType() == ContentType.VIDEO ? MediaType.VIDEO : MediaType.IMAGE;
        }

        if (episode.getContentType() == ContentType.VIDEO && mediaType != MediaType.VIDEO) {
            throw ContentModuleException.badRequest("Video episode only accepts video media URL");
        }
        if (episode.getContentType() == ContentType.COMIC && mediaType != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("Comic episode only accepts image media URLs");
        }
        return mediaType;
    }

    private void validateMediaForEpisode(Episode episode, MediaType mediaType, String currentMediaId) {
        resolveMediaType(episode, mediaType);
        if (episode.getContentType() == ContentType.VIDEO) {
            boolean hasAnotherReadyVideo = currentMediaId == null
                    ? mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                            episode.getEpisodeId(),
                            MediaType.VIDEO,
                            VIDEO_REPLACEMENT_BLOCKING_STATUSES)
                    : mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseAndMediaIdNot(
                            episode.getEpisodeId(),
                            MediaType.VIDEO,
                            VIDEO_REPLACEMENT_BLOCKING_STATUSES,
                            currentMediaId);
            if (hasAnotherReadyVideo) {
                throw ContentModuleException.conflict(
                        "Episode already has an active or processing video media. Delete it first.");
            }
        }
    }

    private String normalizeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw ContentModuleException.badRequest("fileUrl is required");
        }

        String normalizedUrl = fileUrl.trim();
        try {
            URI uri = new URI(normalizedUrl);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw ContentModuleException.badRequest("fileUrl must be a valid HTTP or HTTPS URL");
            }
            return normalizedUrl;
        } catch (URISyntaxException exception) {
            throw ContentModuleException.badRequest("fileUrl must be a valid HTTP or HTTPS URL");
        }
    }

    private String validateMimeType(String mimeType, MediaType mediaType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw ContentModuleException.badRequest("mimeType is required");
        }
        String normalizedMimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        if (mediaType == MediaType.VIDEO) {
            if (!normalizedMimeType.startsWith("video/")
                    && !"application/vnd.apple.mpegurl".equals(normalizedMimeType)) {
                throw ContentModuleException.badRequest("mimeType must be a video MIME type");
            }
            return normalizedMimeType;
        }

        if (!normalizedMimeType.startsWith("image/")) {
            throw ContentModuleException.badRequest("mimeType must be an image MIME type");
        }
        return normalizedMimeType;
    }

    private Long validateFileSize(Long fileSize) {
        if (fileSize == null) {
            throw ContentModuleException.badRequest("fileSize is required");
        }
        if (fileSize < 0) {
            throw ContentModuleException.badRequest("fileSize must be zero or positive");
        }
        return fileSize;
    }

    private void rejectDuplicate(String checksum, String currentMediaId) {
        mediaRepository.findFirstByChecksumAndIsDeletedFalse(checksum)
                .filter(existing -> !existing.getMediaId().equals(currentMediaId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Duplicate media URL detected");
                });
    }

    private void rejectDuplicateChecksums(Collection<String> checksums, String currentMediaId) {
        if (checksums.isEmpty()) {
            return;
        }

        mediaRepository.findAllByChecksumInAndIsDeletedFalse(checksums)
                .stream()
                .filter(existing -> currentMediaId == null || !existing.getMediaId().equals(currentMediaId))
                .findAny()
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Duplicate media URL detected");
                });
    }

    private String checksum(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new ContentModuleException(
                    4301,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot generate URL checksum",
                    exception);
        }
    }

    private String normalizeChecksum(String providedChecksum, String fallbackValue) {
        if (providedChecksum == null || providedChecksum.isBlank()) {
            return checksum(fallbackValue);
        }

        String normalizedChecksum = providedChecksum.trim().toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(normalizedChecksum).matches()) {
            throw ContentModuleException.badRequest("checksum must be a SHA-256 hex string");
        }
        return normalizedChecksum;
    }

    private Integer resolveDisplayOrder(String episodeId, MediaType mediaType, Integer requestedDisplayOrder) {
        if (mediaType == MediaType.VIDEO) {
            return null;
        }
        if (requestedDisplayOrder != null) {
            ensureDisplayOrderAvailable(episodeId, requestedDisplayOrder, null);
            return requestedDisplayOrder;
        }
        return nextDisplayOrder(episodeId);
    }

    private List<Integer> resolveComicDisplayOrders(String episodeId, List<MediaComicPageRequestDto> pages) {
        if (pages.stream().anyMatch(page -> page.getDisplayOrder() == null)) {
            throw ContentModuleException.badRequest("displayOrder is required for every comic page");
        }

        List<Integer> displayOrders = pages.stream()
                .map(MediaComicPageRequestDto::getDisplayOrder)
                .toList();
        Set<Integer> uniqueOrders = new HashSet<>(displayOrders);
        if (uniqueOrders.size() != displayOrders.size()) {
            throw ContentModuleException.badRequest("displayOrder must not contain duplicates");
        }
        if (displayOrders.stream().anyMatch(order -> order < 1)) {
            throw ContentModuleException.badRequest("displayOrder must be positive numbers");
        }

        if (mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndIsDeletedFalse(episodeId, uniqueOrders)) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }
        return displayOrders;
    }

    private void ensureImageMedia(Media media) {
        if (media.getMediaType() != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("displayOrder is only used for comic image media");
        }
    }

    private void ensureDisplayOrderAvailable(String episodeId, Integer displayOrder, String currentMediaId) {
        if (displayOrder == null || displayOrder < 1) {
            throw ContentModuleException.badRequest("displayOrder must be positive numbers");
        }

        boolean exists = currentMediaId == null
                ? mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndIsDeletedFalse(
                        episodeId,
                        List.of(displayOrder))
                : mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndMediaIdNotInAndIsDeletedFalse(
                        episodeId,
                        List.of(displayOrder),
                        List.of(currentMediaId));
        if (exists) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }
    }

    private int nextDisplayOrder(String episodeId) {
        return mediaRepository.findMaxDisplayOrderByEpisodeId(episodeId) + 1;
    }

    private String normalizeStorageProvider(String storageProvider) {
        if (storageProvider == null || storageProvider.isBlank()) {
            return URL_STORAGE_PROVIDER;
        }
        return storageProvider.trim().toUpperCase(Locale.ROOT);
    }

    private MediaProvider resolveProvider(String storageProvider) {
        if (storageProvider == null || storageProvider.isBlank()) {
            return MediaProvider.URL;
        }
        try {
            return MediaProvider.valueOf(storageProvider.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MediaProvider.URL;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Episode lockActiveEpisode(String episodeId) {
        return episodeRepository.lockByEpisodeIdAndIsDeletedFalse(episodeId)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + episodeId));
    }
}
