package com.talex.server.controllers.creator;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.creator.CreatorConfigRequestDto;
import com.talex.server.dtos.responses.creator.CreatorConfigResponseDto;
import com.talex.server.services.config.CreatorConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator-config")
@RequiredArgsConstructor
@Tag(name = "Creator Config", description = "Quản lý tỷ lệ chia sẻ doanh thu cơ bản (Base Ratio)")
public class CreatorConfigController {

    private final CreatorConfigService creatorConfigService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Tạo mới Creator Config (chỉ cho phép tạo 1 lần)")
    public ResponseEntity<BaseResponse> createConfig(@Valid @RequestBody CreatorConfigRequestDto dto) {
        CreatorConfigResponseDto response = creatorConfigService.createConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo Creator Config thành công")
                .data(response)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    @Operation(summary = "Cập nhật Creator Config")
    public ResponseEntity<BaseResponse> updateConfig(@Valid @RequestBody CreatorConfigRequestDto dto) {
        CreatorConfigResponseDto response = creatorConfigService.updateConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật Creator Config thành công")
                .data(response)
                .build());
    }

    @GetMapping
    @Operation(summary = "Lấy thông tin Creator Config hiện tại")
    public ResponseEntity<BaseResponse> getConfig() {
        CreatorConfigResponseDto response = creatorConfigService.getConfigDto();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy Creator Config thành công")
                .data(response)
                .build());
    }
}