package com.talex.server.controllers.config;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.config.AiPipelineConfigRequestDto;
import com.talex.server.services.config.AiPipelineConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AiPipelineConfigController {

    private final AiPipelineConfigService configService;

    @GetMapping("/api/v1/ai-pipeline-configs")
    public ResponseEntity<BaseResponse> getConfig() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy cấu hình AI pipeline thành công")
                .data(configService.getConfig())
                .build());
    }

    @PutMapping("/api/v1/admin/ai-pipeline-configs")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> updateConfig(
            @Valid @RequestBody AiPipelineConfigRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật cấu hình AI pipeline thành công")
                .data(configService.updateConfig(request))
                .build());
    }
}
