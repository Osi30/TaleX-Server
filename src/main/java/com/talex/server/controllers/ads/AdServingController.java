package com.talex.server.controllers.ads;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdTrackRequestDto;
import com.talex.server.services.ads.AdCampaignService;
import com.talex.server.services.ads.AdSlotService;
import com.talex.server.services.ads.AdTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Ad Serving", description = "API serving and tracking ads")
public class AdServingController {

    private final AdCampaignService campaignService;
    private final AdTrackingService trackingService;
    private final AdSlotService slotService;

    @GetMapping("/slots")
    @Operation(summary = "Get active slots")
    public ResponseEntity<BaseResponse> getActiveSlots() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(slotService.getActiveSlots())
                .build());
    }

    @GetMapping("/serve")
    @Operation(summary = "Get ad for serving")
    public ResponseEntity<BaseResponse> serveAd(@RequestParam String slotCode) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.serveAd(slotCode))
                .build());
    }

    @GetMapping("/serve/all")
    @Operation(summary = "Get all ads for serving")
    public ResponseEntity<BaseResponse> serveAllAds(@RequestParam String slotCode) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.serveAllAds(slotCode))
                .build());
    }

    @PostMapping("/track/impression")
    @Operation(summary = "Track impression")
    public ResponseEntity<BaseResponse> trackImpression(
            @Valid @RequestBody AdTrackRequestDto request,
            @CurrentAccountId UUID accountId,
            HttpServletRequest httpRequest
    ) {
        String clientFingerprint = extractClientFingerprint(httpRequest, request);
        trackingService.trackImpressionAsync(request, accountId, clientFingerprint);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Impression tracked asynchronously")
                .build());
    }

    @PostMapping("/track/view-6s")
    @Operation(summary = "Track 6s view")
    public ResponseEntity<BaseResponse> track6sView(
            @Valid @RequestBody AdTrackRequestDto request,
            @CurrentAccountId UUID accountId,
            HttpServletRequest httpRequest
    ) {
        String clientFingerprint = extractClientFingerprint(httpRequest, request);
        trackingService.track6sViewAsync(request, accountId, clientFingerprint);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("6s view tracked asynchronously")
                .build());
    }

    @PostMapping("/track/click")
    @Operation(summary = "Track click")
    public ResponseEntity<BaseResponse> trackClick(
            @Valid @RequestBody AdTrackRequestDto request,
            @CurrentAccountId UUID accountId,
            HttpServletRequest httpRequest
    ) {
        String clientFingerprint = extractClientFingerprint(httpRequest, request);
        trackingService.trackClickAsync(request, accountId, clientFingerprint);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Click tracked asynchronously")
                .build());
    }

    private String extractClientFingerprint(HttpServletRequest request, AdTrackRequestDto dto) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            xfHeader = request.getHeader("X-Real-IP");
        }
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            xfHeader = request.getRemoteAddr();
        }
        String ip = xfHeader.split(",")[0].trim();

        // 1. Priority: Extract hardware deviceId from DTO body or X-Device-Id header
        String deviceId = (dto != null && dto.getDeviceId() != null && !dto.getDeviceId().isBlank())
                ? dto.getDeviceId().trim()
                : request.getHeader("X-Device-Id");

        if (deviceId != null && !deviceId.isBlank()) {
            return ip + "_" + deviceId.trim();
        }

        // 2. Fallback: User-Agent hash
        String userAgent = request.getHeader("User-Agent");
        int uaHash = (userAgent != null) ? userAgent.hashCode() : 0;

        return ip + "_" + uaHash;
    }
}
