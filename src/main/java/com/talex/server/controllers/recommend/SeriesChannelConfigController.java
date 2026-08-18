package com.talex.server.controllers.recommend;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.recommend.request.SeriesChannelConfigReq;
import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import com.talex.server.services.recommend.SeriesChannelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/series-channel-configs")
@RequiredArgsConstructor
public class SeriesChannelConfigController {

    private final SeriesChannelConfigService configService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<BaseResponse> getConfig() {
        SeriesChannelConfigRes result = configService.getConfig();
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin cấu hình SeriesChannelConfig thành công")
                .data(result)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BaseResponse> createConfig(
            @Valid @RequestBody SeriesChannelConfigReq req
    ) throws BadRequestException {
        SeriesChannelConfigRes result = configService.createConfig(req);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo thông tin cấu hình SeriesChannelConfig thành công")
                .data(result)
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<BaseResponse> updateConfig(
            @Valid @RequestBody SeriesChannelConfigReq req
    ) throws BadRequestException {
        SeriesChannelConfigRes result = configService.updateConfig(req);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật cấu hình SeriesChannelConfig thành công")
                .data(result)
                .build());
    }
}