package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ComboEpisodeRequestDto;
import com.talex.server.services.ComboEpisodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Combo Episode", description = "Quản lý Combo Episode dành cho creator")
public class ComboEpisodeController {

    private final ComboEpisodeService comboEpisodeService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/combos")
    @Operation(summary = "Tạo Combo mới", description = "Tạo một combo chứa các tập được chọn")
    public ResponseEntity<BaseResponse> create(
            @Valid @RequestBody ComboEpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Combo created",
                        comboEpisodeService.create(request, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/combos")
    @Operation(summary = "Lấy danh sách Combo của creator", description = "Lấy danh sách các combo thuộc về creator đang đăng nhập")
    public ResponseEntity<BaseResponse> list(
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", comboEpisodeService.listByCreator(accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/combos/{id}")
    @Operation(summary = "Lấy chi tiết Combo", description = "Lấy thông tin chi tiết combo, bao gồm danh sách episode")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", comboEpisodeService.getById(id, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/api/v1/combos/{id}")
    @Operation(summary = "Cập nhật Combo", description = "Cập nhật thông tin combo và danh sách episode")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ComboEpisodeRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Combo updated",
                comboEpisodeService.update(id, request, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/api/v1/combos/{id}")
    @Operation(summary = "Xóa Combo", description = "Xóa mềm combo")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        comboEpisodeService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Combo deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
