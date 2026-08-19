package com.talex.server.controllers.config;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.config.SyncMetadataResponseDto;
import com.talex.server.enums.SyncType;
import com.talex.server.services.config.SyncMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync-metadata")
@RequiredArgsConstructor
@Tag(name = "Sync Metadata", description = "API quản lý trạng thái đồng bộ dữ liệu")
public class SyncMetadataController {

    private final SyncMetadataService syncMetadataService;

    @GetMapping("/{syncType}")
    @Operation(summary = "Lấy thông tin đồng bộ mới nhất theo loại",
            description = "Trả về thời gian đồng bộ cuối cùng đã chuyển đổi sang định dạng LocalDateTime.")
    public ResponseEntity<BaseResponse> getSyncMetadata(@PathVariable SyncType syncType) {
        SyncMetadataResponseDto data = syncMetadataService.getSyncMetadata(syncType);

        if (data == null) {
            return ResponseEntity.ok(BaseResponse.builder()
                    .code(404)
                    .message("Không tìm thấy dữ liệu đồng bộ cho loại: " + syncType)
                    .build());
        }

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin đồng bộ thành công")
                .data(data)
                .build());
    }
}