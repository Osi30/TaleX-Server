package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.SeriesRequestDto;
import com.talex.server.services.SeriesService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/series")
@RequiredArgsConstructor
public class SeriesController {
    private final SeriesService seriesService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> create(
            @Valid @RequestBody SeriesRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Series created", seriesService.create(request, accountId.toString())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", seriesService.list(page, pageSize)));
    }

    @GetMapping("/by-creator")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> listByCreator(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                seriesService.listByCreator(accountId.toString(), page, pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", seriesService.getById(id, accountId.toString())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SeriesRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series updated",
                seriesService.update(id, request, accountId.toString())));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series published", seriesService.publish(id, accountId.toString())));
    }

    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series hidden", seriesService.hide(id, accountId.toString())));
    }

    @PatchMapping("/{id}/unhide")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series visible", seriesService.unhide(id, accountId.toString())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        seriesService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Series deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
