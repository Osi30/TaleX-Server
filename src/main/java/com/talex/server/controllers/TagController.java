package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.TagRequestDto;
import com.talex.server.services.TagService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@Valid @RequestBody TagRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Tag created", tagService.create(request)));
    }

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", tagService.list(page, pageSize)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(response(200, "OK", tagService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody TagRequestDto request) {
        return ResponseEntity.ok(response(200, "Tag updated", tagService.update(id, request)));
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Tag hidden", tagService.hide(id, actorId)));
    }

    @PatchMapping("/{id}/unhide")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        return ResponseEntity.ok(response(200, "Tag visible", tagService.unhide(id, actorId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @RequestParam(required = false) String actorId) {
        tagService.delete(id, actorId);
        return ResponseEntity.ok(response(200, "Tag deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
