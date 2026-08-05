package com.talex.server.controllers.media;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.media.MediaSystemConfigRequestDto;
import com.talex.server.services.media.MediaSystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MediaSystemConfigController {

    private final MediaSystemConfigService configService;

    @GetMapping("/api/v1/media-system-configs")
    public ResponseEntity<BaseResponse> getConfig() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin cấu hình Media thành công")
                .data(configService.getConfig())
                .build());
    }

    @PutMapping("/api/v1/admin/media-system-configs")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> updateConfig(
            @Valid @RequestBody MediaSystemConfigRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật cấu hình Media thành công")
                .data(configService.updateConfig(request))
                .build());
    }
}
