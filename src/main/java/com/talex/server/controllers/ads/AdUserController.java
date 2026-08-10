package com.talex.server.controllers.ads;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdCampaignCreateRequestDto;
import com.talex.server.services.ads.AdCampaignService;
import com.talex.server.services.ads.AdWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.talex.server.services.ads.AdMediaUploadService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/campaigns")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Ad Campaigns", description = "API quản lý chiến dịch quảng cáo của User")
public class AdUserController {

    private final AdCampaignService campaignService;
    private final AdMediaUploadService mediaUploadService;
    private final AdWalletService walletService;

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

    @PostMapping("/profile/setup")
    @Operation(summary = "Thiết lập hồ sơ doanh nghiệp", description = "Lưu thông tin công ty lần đầu vào dashboard")
    public ResponseEntity<BaseResponse> setupProfile(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody com.talex.server.dtos.requests.ads.AdProfileSetupRequestDto request) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Profile setup completed")
                .data(walletService.setupProfile(accountId, request))
                .build());
    }

    @PatchMapping("/{campaignId}/labels")
    @Operation(summary = "Cập nhật nhãn của chiến dịch", description = "User tự gắn nhãn cho chiến dịch của mình")
    public ResponseEntity<BaseResponse> updateCampaignLabels(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId,
            @RequestBody java.util.List<String> labels) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Labels updated successfully")
                .data(campaignService.updateCampaignLabels(accountId, campaignId, labels))
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

    @GetMapping("/{campaignId}/transactions")
    @Operation(summary = "Lấy lịch sử giao dịch của chiến dịch", description = "Lấy danh sách các lần trừ tiền lượt xem của chiến dịch này")
    public ResponseEntity<BaseResponse> getCampaignTransactions(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                // Using a quick query via repository or adding it to campaignService
                // Ideally should add to campaignService:
                .data(campaignService.getCampaignTransactions(accountId, campaignId))
                .build());
    }

    @PatchMapping("/{campaignId}/toggle")
    @Operation(summary = "Bật/tắt chiến dịch quảng cáo", description = "Chuyển trạng thái giữa ACTIVE và PAUSED")
    public ResponseEntity<BaseResponse> toggleCampaign(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId) {
        campaignService.toggleCampaign(accountId, campaignId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign toggled successfully")
                .build());
    }

    @PostMapping("/{campaignId}/cancel")
    @Operation(summary = "Hủy chiến dịch quảng cáo", description = "Hủy và hoàn tiền vào ví nếu chiến dịch chưa kết thúc")
    public ResponseEntity<BaseResponse> cancelCampaign(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId) {
        campaignService.cancelCampaign(accountId, campaignId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign cancelled and refunded successfully")
                .build());
    }

    @PatchMapping("/{campaignId}/schedule")
    @Operation(summary = "Cập nhật lịch chạy quảng cáo", description = "Đổi lịch khi chiến dịch đang Tạm dừng")
    public ResponseEntity<BaseResponse> updateCampaignSchedule(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId,
            @RequestBody com.talex.server.dtos.requests.ads.AdCampaignScheduleRequestDto request) {
        campaignService.updateCampaignSchedule(accountId, campaignId, request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign schedule updated successfully")
                .build());
    }

    @PostMapping("/{campaignId}/topup")
    @Operation(summary = "Nạp thêm tiền cho chiến dịch", description = "Lấy tiền từ Master Wallet nạp vào Campaign Balance")
    public ResponseEntity<BaseResponse> topupCampaign(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID campaignId,
            @RequestBody java.util.Map<String, Long> payload) {
        Long amount = payload.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        campaignService.topupCampaign(accountId, campaignId, amount);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign topped up successfully")
                .build());
    }
}
