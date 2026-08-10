package com.talex.server.controllers.admin;

import com.talex.server.dtos.responses.ApiResponse;
import com.talex.server.dtos.responses.admin.AdminWatermarkResponseDto;
import com.talex.server.services.admin.AdminWatermarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/watermark")
@RequiredArgsConstructor
public class AdminWatermarkController {

    private final AdminWatermarkService adminWatermarkService;

    @PostMapping("/extract")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<AdminWatermarkResponseDto>> extractWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("media_type") String mediaType) {
        
        AdminWatermarkResponseDto response = adminWatermarkService.extractWatermark(file, mediaType);
        
        return ResponseEntity.ok(ApiResponse.ok("Trích xuất bản quyền thành công", response));
    }
}
