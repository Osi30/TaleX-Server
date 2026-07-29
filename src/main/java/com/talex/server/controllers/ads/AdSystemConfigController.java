package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.ads.impls.AdSystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ads/config")
@RequiredArgsConstructor
@Tag(name = "Ad System Config", description = "API cấu hình hệ thống quảng cáo")
public class AdSystemConfigController {

    private final AdSystemConfigService configService;

    /**
     * PUBLIC — FE gọi để biết cần hiện popup ở những trang nào.
     */
    @GetMapping("/popup")
    @Operation(
        summary = "Lấy cấu hình Popup (Routes & Delay)",
        description = "Frontend gọi khi khởi động để lấy cấu hình từ DB."
    )
    public ResponseEntity<BaseResponse> getPopupConfig() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(configService.getPopupConfig())
                .build());
    }

    @PutMapping("/popup")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
        summary = "Cập nhật cấu hình Popup (Admin)",
        description = "Admin cập nhật danh sách route và thời gian delay."
    )
    public ResponseEntity<BaseResponse> updatePopupConfig(@RequestBody com.talex.server.dtos.requests.ads.PopupConfigDto config) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Popup config updated successfully")
                .data(configService.updatePopupConfig(config))
                .build());
    }
}
