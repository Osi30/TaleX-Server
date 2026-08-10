package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdSlotRequestDto;
import com.talex.server.services.ads.AdSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/admin/slots")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Ad Admin Slots", description = "API quản lý vị trí quảng cáo dành cho Admin")
public class AdAdminSlotController {

    private final AdSlotService slotService;

    @GetMapping
    @Operation(summary = "Lấy tất cả các Slots", description = "Admin xem tất cả các cấu hình vị trí.")
    public ResponseEntity<BaseResponse> getAllSlots() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(slotService.getAllSlots())
                .build());
    }

    // @PostMapping
    // @Operation(summary = "Tạo cấu hình Slot mới", description = "Admin tạo thêm vị trí cho phép user đặt QC.")
    // public ResponseEntity<BaseResponse> createSlot(@Valid @RequestBody AdSlotRequestDto request) {
    //     return ResponseEntity.ok(BaseResponse.builder()
    //             .code(200)
    //             .message("Slot created")
    //             .data(slotService.createSlot(request))
    //             .build());
    // }

    @PutMapping("/{slotId}")
    @Operation(summary = "Cập nhật cấu hình Slot", description = "Admin đổi giá, đổi tên Slot.")
    public ResponseEntity<BaseResponse> updateSlot(
            @PathVariable UUID slotId,
            @Valid @RequestBody AdSlotRequestDto request) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Slot updated")
                .data(slotService.updateSlot(slotId, request))
                .build());
    }

    @PatchMapping("/{slotId}/status")
    @Operation(summary = "Bật/Tắt Slot", description = "Admin ẩn/hiện vị trí QC.")
    public ResponseEntity<BaseResponse> toggleSlotStatus(
            @PathVariable UUID slotId,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Slot status updated")
                .data(slotService.toggleSlotStatus(slotId, isActive))
                .build());
    }

    // @DeleteMapping("/{slotId}")
    // @Operation(summary = "Xoá cấu hình Slot", description = "Admin xoá vị trí QC.")
    // public ResponseEntity<BaseResponse> deleteSlot(@PathVariable UUID slotId) {
    //     slotService.deleteSlot(slotId);
    //     return ResponseEntity.ok(BaseResponse.builder()
    //             .code(200)
    //             .message("Slot deleted")
    //             .build());
    // }
}
