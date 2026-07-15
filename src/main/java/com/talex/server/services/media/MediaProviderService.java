package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.ImagePresignedUploadRequestDto;
import com.talex.server.dtos.requests.media.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.responses.media.ImagePresignedUploadResponseDto;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaUploadSession;
import com.talex.server.services.media.impls.SignedUploadParams;

import java.time.LocalDateTime;

public interface MediaProviderService {
    SignedUploadParams createSignedUploadParams(String providerPublicId, String providerDeliveryType);

    ImagePresignedUploadResponseDto createImagePresignedUpload(ImagePresignedUploadRequestDto request);

    String buildVideoPublicId(String episodeId, String mediaId);

    void applyCompletedUpload(Media media, MediaUploadSession session, MediaUploadCompleteRequestDto request);

    String buildHlsUrl(Media media);

    String buildSignedHlsUrl(Media media, LocalDateTime expiresAt);

    String buildThumbnailUrl(Media media);

    /** Sign a single URL (exact resource, canned policy) for protected content like thumbnails */
    String signSingleUrl(String url, LocalDateTime expiresAt);

    void deleteAsset(Media media);
}
