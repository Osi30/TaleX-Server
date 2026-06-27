package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.requests.ScheduledPublishRequestDto;
import com.talex.server.services.EpisodeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EpisodeController {
    private final EpisodeService episodeService;

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PostMapping("/api/v1/seasons/{seasonId}/episodes")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String seasonId,
            @Valid @RequestBody EpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Episode created",
                        episodeService.create(seasonId, request, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @GetMapping("/api/v1/seasons/{seasonId}/episodes")
    public ResponseEntity<BaseResponse> listBySeason(
            @PathVariable String seasonId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                episodeService.listBySeason(seasonId, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @GetMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", episodeService.getById(id, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PutMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody EpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode updated",
                episodeService.update(id, request, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PatchMapping("/api/v1/episodes/{id}/schedule-publish")
    public ResponseEntity<BaseResponse> schedulePublish(
            @PathVariable String id,
            @Valid @RequestBody ScheduledPublishRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode publish scheduled",
                episodeService.schedulePublish(id, request.getScheduledPublishAt(), accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PatchMapping("/api/v1/episodes/{id}/publish")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode published", episodeService.publish(id, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PatchMapping("/api/v1/episodes/{id}/hide")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode hidden", episodeService.hide(id, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @PatchMapping("/api/v1/episodes/{id}/unhide")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Episode visible", episodeService.unhide(id, accountId.toString())));
    }

    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @DeleteMapping("/api/v1/episodes/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        episodeService.delete(id, accountId.toString());
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
