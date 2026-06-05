package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.services.SeasonService;
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
public class SeasonController {
    private final SeasonService seasonService;

    @PostMapping("/api/v1/series/{seriesId}/seasons")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String seriesId,
            @Valid @RequestBody SeasonRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Season created", seasonService.create(seriesId, request)));
    }

    @GetMapping("/api/v1/series/{seriesId}/seasons")
    public ResponseEntity<BaseResponse> listBySeries(@PathVariable String seriesId) {
        return ResponseEntity.ok(response(200, "OK", seasonService.listBySeries(seriesId)));
    }

    @GetMapping("/api/v1/seasons/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(response(200, "OK", seasonService.getById(id)));
    }

    @PutMapping("/api/v1/seasons/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SeasonRequestDto request) {
        return ResponseEntity.ok(response(200, "Season updated", seasonService.update(id, request)));
    }

    @PatchMapping("/api/v1/seasons/{id}/publish")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Season published", seasonService.publish(id, actorId)));
    }

    @PatchMapping("/api/v1/seasons/{id}/hide")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Season hidden", seasonService.hide(id, actorId)));
    }

    @PatchMapping("/api/v1/seasons/{id}/unhide")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Season visible", seasonService.unhide(id, actorId)));
    }

    @DeleteMapping("/api/v1/seasons/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        seasonService.delete(id, actorId);
        return ResponseEntity.ok(response(200, "Season deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
