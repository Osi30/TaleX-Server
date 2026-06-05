package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.VideoUploadSessionRequestDto;
import com.talex.server.services.MediaService;
import com.talex.server.services.MediaUploadSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final MediaUploadSessionService mediaUploadSessionService;

    @PostMapping("/api/v1/episodes/{episodeId}/media/video/upload-session")
    public ResponseEntity<BaseResponse> createVideoUploadSession(
            @PathVariable String episodeId,
            @Valid @RequestBody VideoUploadSessionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Video upload session created",
                        mediaUploadSessionService.createVideoUploadSession(episodeId, request)));
    }

    @GetMapping("/api/v1/media/upload-sessions/{uploadSessionId}")
    public ResponseEntity<BaseResponse> getUploadSession(@PathVariable String uploadSessionId) {
        return ResponseEntity.ok(response(200, "OK", mediaUploadSessionService.getSession(uploadSessionId)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/progress")
    public ResponseEntity<BaseResponse> updateUploadProgress(
            @PathVariable String uploadSessionId,
            @Valid @RequestBody MediaUploadProgressRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload progress updated",
                mediaUploadSessionService.updateProgress(uploadSessionId, request)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/pause")
    public ResponseEntity<BaseResponse> pauseUpload(
            @PathVariable String uploadSessionId,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Upload paused",
                mediaUploadSessionService.pause(uploadSessionId, actorId)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/fail")
    public ResponseEntity<BaseResponse> failUpload(
            @PathVariable String uploadSessionId,
            @RequestBody(required = false) MediaUploadFailRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload failed",
                mediaUploadSessionService.fail(uploadSessionId, request)));
    }

    @PatchMapping("/api/v1/media/upload-sessions/{uploadSessionId}/cancel")
    public ResponseEntity<BaseResponse> cancelUpload(
            @PathVariable String uploadSessionId,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Upload cancelled",
                mediaUploadSessionService.cancel(uploadSessionId, actorId)));
    }

    @PostMapping("/api/v1/media/upload-sessions/{uploadSessionId}/complete")
    public ResponseEntity<BaseResponse> completeUpload(
            @PathVariable String uploadSessionId,
            @Valid @RequestBody MediaUploadCompleteRequestDto request) {
        return ResponseEntity.ok(response(200, "Upload completed",
                mediaUploadSessionService.complete(uploadSessionId, request)));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/media")
    public ResponseEntity<BaseResponse> createFromUrl(
            @PathVariable String episodeId,
            @RequestBody MediaMetadataRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Media URL created", mediaService.createFromUrl(episodeId, request)));
    }

    @PostMapping("/api/v1/episodes/{episodeId}/media/comic-pages")
    public ResponseEntity<BaseResponse> createComicPagesFromUrls(
            @PathVariable String episodeId,
            @RequestBody MediaComicPagesRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Comic page URLs created",
                        mediaService.createComicPagesFromUrls(episodeId, request)));
    }

    @GetMapping("/api/v1/episodes/{episodeId}/media")
    public ResponseEntity<BaseResponse> listByEpisode(@PathVariable String episodeId) {
        return ResponseEntity.ok(response(200, "OK", mediaService.listByEpisode(episodeId)));
    }

    @GetMapping("/api/v1/media/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(response(200, "OK", mediaService.getById(id)));
    }

    @PutMapping("/api/v1/media/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @RequestBody MediaUpdateRequestDto request) {
        return ResponseEntity.ok(response(200, "Media updated", mediaService.update(id, request)));
    }

    @PutMapping("/api/v1/media/{id}/url")
    public ResponseEntity<BaseResponse> replaceUrl(
            @PathVariable String id,
            @RequestBody MediaMetadataRequestDto request) {
        return ResponseEntity.ok(response(200, "Media URL replaced", mediaService.replaceUrl(id, request)));
    }

    @PutMapping("/api/v1/episodes/{episodeId}/media/reorder")
    public ResponseEntity<BaseResponse> reorder(
            @PathVariable String episodeId,
            @Valid @RequestBody MediaReorderRequestDto request) {
        return ResponseEntity.ok(response(200, "Media reordered", mediaService.reorder(episodeId, request)));
    }

    @PatchMapping("/api/v1/media/{id}/hide")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Media hidden", mediaService.hide(id, actorId)));
    }

    @PatchMapping("/api/v1/media/{id}/unhide")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Media visible", mediaService.unhide(id, actorId)));
    }

    @PatchMapping("/api/v1/media/{id}/status")
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody MediaStatusRequestDto request) {
        return ResponseEntity.ok(response(200, "Media status updated", mediaService.updateProcessingStatus(id, request)));
    }

    @DeleteMapping("/api/v1/media/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        mediaService.delete(id, actorId);
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
