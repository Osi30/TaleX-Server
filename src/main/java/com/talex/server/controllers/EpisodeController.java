package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.services.EpisodeService;
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
public class EpisodeController {
    private final EpisodeService episodeService;

    @PostMapping("/api/v1/seasons/{seasonId}/episodes")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String seasonId,
            @Valid @RequestBody EpisodeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Episode created", episodeService.create(seasonId, request)));
    }

    @GetMapping("/api/v1/seasons/{seasonId}/episodes")
    public ResponseEntity<BaseResponse> listBySeason(@PathVariable String seasonId) {
        return ResponseEntity.ok(response(200, "OK", episodeService.listBySeason(seasonId)));
    }

    @GetMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(response(200, "OK", episodeService.getById(id)));
    }

    @PutMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody EpisodeRequestDto request) {
        return ResponseEntity.ok(response(200, "Episode updated", episodeService.update(id, request)));
    }

    @PatchMapping("/api/v1/episodes/{id}/publish")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Episode published", episodeService.publish(id, actorId)));
    }

    @PatchMapping("/api/v1/episodes/{id}/hide")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Episode hidden", episodeService.hide(id, actorId)));
    }

    @PatchMapping("/api/v1/episodes/{id}/unhide")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Episode visible", episodeService.unhide(id, actorId)));
    }

    @DeleteMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        episodeService.delete(id, actorId);
        return ResponseEntity.ok(response(200, "Episode deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
