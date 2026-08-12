package com.talex.server.controllers.recommend;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.request.TrendingSampleConfigReq;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import com.talex.server.services.trending.TrendingSampleConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trending-configs")
@RequiredArgsConstructor
public class TrendingSampleConfigController {

    private final TrendingSampleConfigService configService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<BaseResponse> getConfig() {
        TrendingSampleConfigRes result = configService.getConfig();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin cấu hình thành công")
                .data(result)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BaseResponse> createConfig(
            @Valid @RequestBody TrendingSampleConfigReq req
    ) throws BadRequestException {
        TrendingSampleConfigRes result = configService.createConfig(req);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo thông tin cấu hình thành công")
                .data(result)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<BaseResponse> updateConfig(
            @Valid @RequestBody TrendingSampleConfigReq req
    ) throws BadRequestException {
        TrendingSampleConfigRes result = configService.updateConfig(req);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật cấu hình thành công")
                .data(result)
                .build());
    }
}