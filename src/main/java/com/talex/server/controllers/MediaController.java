package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ImagePresignedUploadRequestDto;
import com.talex.server.dtos.requests.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaRejectRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.VideoUploadSessionRequestDto;
import com.talex.server.services.MediaService;
import com.talex.server.services.MediaPlaybackSecurityService;
import com.talex.server.services.MediaUploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final MediaUploadSessionService mediaUploadSessionService;
    private final MediaPlaybackSecurityService mediaPlaybackSecurityService;
    private final com.talex.server.services.media.MediaProviderService mediaProviderService;

    @PostMapping("/api/v1/media/image/presigned-upload")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> createImagePresignedUpload(
            @Valid @RequestBody ImagePresignedUploadRequestDto request) {
        return ResponseEntity.ok(response(200, "Image presigned upload URL created",
                mediaProviderService.createImagePresignedUpload(request)));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/media/video/upload-session")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> createVideoUploadSession(
            @PathVariable String episodeId,
            @Valid @RequestBody VideoUploadSessionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Video upload session created",
                        mediaUploadSessionService.createVideoUploadSession(episodeId, request)));
    }

    @GetMapping("/api/v1/media/upload-sessions/{uploadSessionId}")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getUploadSession(@PathVariable String uploadSessionId) {
        return ResponseEntity.ok(response(200, "OK", mediaUploadSessionService.getSession(uploadSessionId)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/progress")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> updateUploadProgress(
            @PathVariable String uploadSessionId,
            @Valid @RequestBody MediaUploadProgressRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload progress updated",
                mediaUploadSessionService.updateProgress(uploadSessionId, request)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/pause")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> pauseUpload(
            @PathVariable String uploadSessionId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Upload paused",
                mediaUploadSessionService.pause(uploadSessionId, accountId.toString())));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/fail")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> failUpload(
            @PathVariable String uploadSessionId,
            @RequestBody(required = false) MediaUploadFailRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload failed",
                mediaUploadSessionService.fail(uploadSessionId, request)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/cancel")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> cancelUpload(
            @PathVariable String uploadSessionId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Upload cancelled",
                mediaUploadSessionService.cancel(uploadSessionId, accountId.toString())));
    }

    @PostMapping("/api/v1/media/upload-sessions/{uploadSessionId}/complete")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> completeUpload(
            @PathVariable String uploadSessionId,
            @Valid @RequestBody MediaUploadCompleteRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload completed",
                mediaUploadSessionService.complete(uploadSessionId, request)));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/media")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> createFromUrl(
            @PathVariable String episodeId,
            @RequestBody MediaMetadataRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Media URL created", mediaService.createFromUrl(episodeId, request, accountId.toString())));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/media/comic-pages")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> createComicPagesFromUrls(
            @PathVariable String episodeId,
            @RequestBody MediaComicPagesRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Comic page URLs created",
                        mediaService.createComicPagesFromUrls(episodeId, request, accountId.toString())));
    }

    @GetMapping("/api/v1/episodes/{episodeId}/media")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> listByEpisode(
            @PathVariable String episodeId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", mediaService.listByEpisode(episodeId, accountId.toString())));
    }

    @GetMapping("/api/v1/episodes/{episodeId}/playback")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getCreatorEpisodePlayback(
            @PathVariable String episodeId,
            @RequestParam(required = false) String viewerId,
            HttpServletRequest request) {
        return ResponseEntity.ok(response(200, "OK",
                mediaPlaybackSecurityService.getCreatorEpisodePlayback(
                        episodeId,
                        viewerId,
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"))));
    }

    @GetMapping("/api/v1/media/{id}")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", mediaService.getById(id, accountId.toString())));
    }

    // Returns copyright + moderation violations for a specific media (creator view)
    @GetMapping("/api/v1/media/{mediaId}/violations")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getMediaViolations(@PathVariable String mediaId) {
        return ResponseEntity.ok(response(200, "OK", mediaService.getMediaViolations(mediaId)));
    }

    // Paginated list of media pending staff review
    @GetMapping("/api/v1/media/pending-review")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> listPendingReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(response(200, "OK", mediaService.listPendingReview(page, size)));
    }

    @PutMapping("/api/v1/media/{id}")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @RequestBody MediaUpdateRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media updated", mediaService.update(id, request, accountId.toString())));
    }

    @PutMapping("/api/v1/media/{id}/url")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> replaceUrl(
            @PathVariable String id,
            @RequestBody MediaMetadataRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media URL replaced", mediaService.replaceUrl(id, request, accountId.toString())));
    }

    @PutMapping("/api/v1/episodes/{episodeId}/media/reorder")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> reorder(
            @PathVariable String episodeId,
            @Valid @RequestBody MediaReorderRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media reordered", mediaService.reorder(episodeId, request, accountId.toString())));
    }

    @PatchMapping("/api/v1/media/{id}/hide")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media hidden", mediaService.hide(id, accountId.toString())));
    }

    @PatchMapping("/api/v1/media/{id}/unhide")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media visible", mediaService.unhide(id, accountId.toString())));
    }

    @PatchMapping("/api/v1/media/{id}/approve")
//    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> approve(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media approved", mediaService.approve(id, accountId.toString())));
    }

    @PatchMapping("/api/v1/media/{id}/reject")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> reject(
            @PathVariable String id,
            @CurrentAccountId UUID accountId,
            @RequestBody(required = false) MediaRejectRequestDto request) {
        return ResponseEntity.ok(response(200, "Media rejected",
                mediaService.rejectWithReason(id, accountId.toString(), request)));
    }

    @PatchMapping("/api/v1/media/{id}/status")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody MediaStatusRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Media status updated", mediaService.updateProcessingStatus(id, request, accountId.toString())));
    }

    @DeleteMapping("/api/v1/media/{id}")
     @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        mediaService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Media deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
