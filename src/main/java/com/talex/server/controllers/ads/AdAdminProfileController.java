package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.ads.AdWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/admin/profiles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Ad Admin Profiles", description = "API quản lý hồ sơ nhà quảng cáo dành cho Admin")
public class AdAdminProfileController {

    private final AdWalletService walletService;

    @GetMapping
    @Operation(summary = "Lấy tất cả hồ sơ nhà quảng cáo", description = "Admin xem danh sách các nhà quảng cáo")
    public ResponseEntity<BaseResponse> getAllProfiles() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(walletService.getAllProfilesForAdmin())
                .build());
    }

    @PatchMapping("/{profileId}/lock")
    @Operation(summary = "Khóa hoặc Mở khóa tài khoản quảng cáo", description = "Admin khóa tài khoản để ngăn người dùng tạo/chạy quảng cáo mới")
    public ResponseEntity<BaseResponse> toggleLockProfile(
            @PathVariable UUID profileId,
            @RequestParam boolean isLocked) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message(isLocked ? "Đã khóa tài khoản quảng cáo" : "Đã mở khóa tài khoản quảng cáo")
                .data(walletService.toggleLockProfile(profileId, isLocked))
                .build());
    }
}
