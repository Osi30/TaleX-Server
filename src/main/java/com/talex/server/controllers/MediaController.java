package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.services.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping(value = "/api/v1/episodes/{episodeId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> upload(
            @PathVariable String episodeId,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute MediaMetadataRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Media uploaded", mediaService.upload(episodeId, file, request)));
    }

    @PostMapping(value = "/api/v1/episodes/{episodeId}/media/comic-pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> uploadComicPages(
            @PathVariable String episodeId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("displayOrders") List<Integer> displayOrders,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Comic pages uploaded",
                        mediaService.uploadComicPages(episodeId, files, displayOrders, actorId)));
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

    @PutMapping(value = "/api/v1/media/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> replaceFile(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute MediaMetadataRequestDto request) {
        return ResponseEntity.ok(response(200, "Media file replaced", mediaService.replaceFile(id, file, request)));
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
