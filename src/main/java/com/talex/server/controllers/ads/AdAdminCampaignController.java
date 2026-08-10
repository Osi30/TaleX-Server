package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdCampaignReviewRequestDto;
import com.talex.server.services.ads.AdCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/admin/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Ad Admin Campaigns", description = "API quản lý chiến dịch quảng cáo dành cho Admin")
public class AdAdminCampaignController {

    private final AdCampaignService campaignService;

    @GetMapping("/pending")
    @Operation(summary = "Lấy danh sách chiến dịch chờ duyệt", description = "Admin xem các quảng cáo user vừa nộp.")
    public ResponseEntity<BaseResponse> getPendingCampaigns() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.getPendingCampaigns())
                .build());
    }

    @PostMapping("/{campaignId}/review")
    @Operation(summary = "Duyệt hoặc Từ chối chiến dịch", description = "Truyền ACTIVE để duyệt (Trừ tiền user), hoặc REJECTED để từ chối (Hoàn tiền cho user).")
    public ResponseEntity<BaseResponse> reviewCampaign(
            @PathVariable UUID campaignId,
            @Valid @RequestBody AdCampaignReviewRequestDto request) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign reviewed")
                .data(campaignService.reviewCampaign(campaignId, request))
                .build());
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả chiến dịch", description = "Admin xem tất cả các chiến dịch trong hệ thống.")
    public ResponseEntity<BaseResponse> getAllCampaigns() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.getAllCampaignsForAdmin())
                .build());
    }

    @PatchMapping("/{campaignId}/status")
    @Operation(summary = "Đổi trạng thái chiến dịch", description = "Admin Bật/Tắt (ACTIVE/PAUSED) chiến dịch.")
    public ResponseEntity<BaseResponse> patchCampaignStatus(
            @PathVariable UUID campaignId,
            @RequestParam com.talex.server.enums.ads.AdCampaignStatus status) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign status updated")
                .data(campaignService.patchCampaignStatus(campaignId, status))
                .build());
    }
}
