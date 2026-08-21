package com.talex.server.controllers.config;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.config.SettlementConfigRequestDto;
import com.talex.server.dtos.responses.config.SettlementConfigResponseDto;
import com.talex.server.services.config.SettlementConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settlement-config")
@RequiredArgsConstructor
@Tag(name = "Settlement Config", description = "Quản lý cấu hình quyết toán")
public class SettlementConfigController {

    private final SettlementConfigService settlementConfigService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới cấu hình quyết toán (Chỉ tạo được 1 lần)")
    public ResponseEntity<BaseResponse> createSettlementConfig(@Valid @RequestBody SettlementConfigRequestDto dto) {
        SettlementConfigResponseDto response = settlementConfigService.createSettlementConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo SettlementConfig thành công")
                .data(response)
                .build());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật cấu hình quyết toán")
    public ResponseEntity<BaseResponse> updateSettlementConfig(@Valid @RequestBody SettlementConfigRequestDto dto) {
        SettlementConfigResponseDto response = settlementConfigService.updateSettlementConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật SettlementConfig thành công")
                .data(response)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Xem thông tin cấu hình quyết toán hiện tại")
    public ResponseEntity<BaseResponse> getSettlementConfig() {
        SettlementConfigResponseDto response = settlementConfigService.getSettlementConfigDto();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin SettlementConfig thành công")
                .data(response)
                .build());
    }
}