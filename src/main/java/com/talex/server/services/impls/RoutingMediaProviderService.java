package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.ImagePresignedUploadRequestDto;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.responses.ImagePresignedUploadResponseDto;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaUploadSession;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.services.media.MediaPackagingService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.SignedUploadParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Primary
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingMediaProviderService implements MediaProviderService, MediaPackagingService {

    private final MediaProperties mediaProperties;
    private final CloudinaryMediaProviderService cloudinaryProvider;
    private final S3MediaProviderService s3Provider;

    private MediaProviderService getActiveProvider() {
        MediaProvider active = mediaProperties.getProvider();
        if (active == MediaProvider.AWS) {
            return s3Provider;
        }
        return cloudinaryProvider;
    }

    private MediaProviderService getProviderForMedia(MediaProvider storedProvider) {
        if (storedProvider == MediaProvider.AWS) {
            return s3Provider;
        }
        return cloudinaryProvider;
    }

    @Override
    public ImagePresignedUploadResponseDto createImagePresignedUpload(ImagePresignedUploadRequestDto request) {
        return getActiveProvider().createImagePresignedUpload(request);
    }

    @Override
    public SignedUploadParams createSignedUploadParams(String providerPublicId, String providerDeliveryType) {
        return getActiveProvider().createSignedUploadParams(providerPublicId, providerDeliveryType);
    }

    @Override
    public String buildVideoPublicId(String episodeId, String mediaId) {
        return getActiveProvider().buildVideoPublicId(episodeId, mediaId);
    }

    @Override
    public void applyCompletedUpload(Media media, MediaUploadSession session, MediaUploadCompleteRequestDto request) {
        getProviderForMedia(media.getProvider()).applyCompletedUpload(media, session, request);
    }

    @Override
    public String buildHlsUrl(Media media) {
        return getProviderForMedia(media.getProvider()).buildHlsUrl(media);
    }

    @Override
    public String buildSignedHlsUrl(Media media, LocalDateTime expiresAt) {
        return getProviderForMedia(media.getProvider()).buildSignedHlsUrl(media, expiresAt);
    }

    @Override
    public String buildThumbnailUrl(Media media) {
        return getProviderForMedia(media.getProvider()).buildThumbnailUrl(media);
    }

    @Override
    public String signSingleUrl(String url, LocalDateTime expiresAt) {
        return getActiveProvider().signSingleUrl(url, expiresAt);
    }

    @Override
    public void deleteAsset(Media media) {
        getProviderForMedia(media.getProvider()).deleteAsset(media);
    }

    @Override
    public String createHlsPackaging(Media media) {
        MediaProviderService provider = getProviderForMedia(media.getProvider());
        if (provider instanceof MediaPackagingService pkg) {
            return pkg.createHlsPackaging(media);
        }
        return null;
    }

    @Override
    public String createDashPackaging(Media media) {
        MediaProviderService provider = getProviderForMedia(media.getProvider());
        if (provider instanceof MediaPackagingService pkg) {
            return pkg.createDashPackaging(media);
        }
        return null;
    }

    @Override
    public String getManifestUrl(Media media) {
        MediaProviderService provider = getProviderForMedia(media.getProvider());
        if (provider instanceof MediaPackagingService pkg) {
            return pkg.getManifestUrl(media);
        }
        return null;
    }
}
