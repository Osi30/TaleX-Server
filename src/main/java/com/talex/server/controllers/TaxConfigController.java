package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.TaxConfigRequestDto;
import com.talex.server.dtos.responses.TaxConfigResponseDto;
import com.talex.server.services.TaxConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-config")
@RequiredArgsConstructor
@Tag(name = "Tax Config", description = "Quản lý cấu hình thuế (VAT, PIT)")
public class TaxConfigController {

    private final TaxConfigService taxConfigService;

    @PostMapping
    @Operation(summary = "Tạo mới cấu hình thuế (Chỉ tạo được 1 lần)")
    public ResponseEntity<BaseResponse> createTaxConfig(@Valid @RequestBody TaxConfigRequestDto dto) {
        TaxConfigResponseDto response = taxConfigService.createTaxConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo TaxConfig thành công")
                .data(response)
                .build());
    }

    @PutMapping
    @Operation(summary = "Cập nhật cấu hình thuế")
    public ResponseEntity<BaseResponse> updateTaxConfig(@Valid @RequestBody TaxConfigRequestDto dto) {
        TaxConfigResponseDto response = taxConfigService.updateTaxConfig(dto);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật TaxConfig thành công")
                .data(response)
                .build());
    }

    @GetMapping
    @Operation(summary = "Xem thông tin cấu hình thuế hiện tại")
    public ResponseEntity<BaseResponse> getTaxConfig() {
        TaxConfigResponseDto response = taxConfigService.getTaxConfigDto();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin TaxConfig thành công")
                .data(response)
                .build());
    }
}