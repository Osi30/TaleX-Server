package com.talex.server.controllers.ads;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdCampaignCreateRequestDto;
import com.talex.server.services.ads.IAdCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.talex.server.services.ads.IAdMediaUploadService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/campaigns")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Ad Campaigns", description = "API quản lý chiến dịch quảng cáo của User")
public class AdUserController {

    private final IAdCampaignService campaignService;
    private final IAdMediaUploadService mediaUploadService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload file quảng cáo (Banner/Video) lên S3", description = "User tải file ảnh/video lên, trả về link URL gốc S3.")
    public ResponseEntity<BaseResponse> uploadAdMedia(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("File uploaded successfully")
                .data(mediaUploadService.uploadAdMedia(file))
                .build());
    }

    @PostMapping
    @Operation(summary = "Tạo chiến dịch quảng cáo mới", description = "User mua quảng cáo. Yêu cầu ví phải đủ tiền để HOLD.")
    public ResponseEntity<BaseResponse> createCampaign(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody AdCampaignCreateRequestDto request) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign created successfully")
                .data(campaignService.createCampaign(accountId, request))
                .build());
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách chiến dịch của tôi", description = "Xem lịch sử quảng cáo của User đang đăng nhập.")
    public ResponseEntity<BaseResponse> getMyCampaigns(@CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.getMyCampaigns(accountId))
                .build());
    }

    @GetMapping("/{campaignId}/metrics")
    @Operation(summary = "Lấy thống kê biểu đồ của 1 chiến dịch", description = "Trả về lịch sử View/Click theo từng ngày")
    public ResponseEntity<BaseResponse> getCampaignMetrics(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.getCampaignMetrics(accountId, campaignId))
                .build());
    }
}
