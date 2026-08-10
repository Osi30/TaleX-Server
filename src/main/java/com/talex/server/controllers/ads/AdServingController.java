package com.talex.server.controllers.ads;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdTrackRequestDto;
import com.talex.server.services.ads.AdCampaignService;
import com.talex.server.services.ads.AdSlotService;
import com.talex.server.services.ads.AdTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Ad Serving", description = "API công khai để phân phối quảng cáo và tracking")
public class AdServingController {

    private final AdCampaignService campaignService;
    private final AdTrackingService trackingService;
    private final AdSlotService slotService;

    @GetMapping("/slots")
    @Operation(summary = "Lấy danh sách Slot đang kích hoạt", description = "Dùng bởi Frontend để User chọn vị trí muốn quảng cáo.")
    public ResponseEntity<BaseResponse> getActiveSlots() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(slotService.getActiveSlots())
                .build());
    }

    @GetMapping("/serve")
    @Operation(summary = "Lấy quảng cáo để hiển thị", description = "Dùng bởi Frontend để load quảng cáo ngẫu nhiên cho một vị trí.")
    public ResponseEntity<BaseResponse> serveAd(@RequestParam String slotCode) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.serveAd(slotCode))
                .build());
    }

    @GetMapping("/serve/all")
    @Operation(summary = "Lấy tất cả quảng cáo để hiển thị", description = "Dùng bởi Frontend để load toàn bộ quảng cáo cho một vị trí (ví dụ: Carousel).")
    public ResponseEntity<BaseResponse> serveAllAds(@RequestParam String slotCode) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(campaignService.serveAllAds(slotCode))
                .build());
    }

    @PostMapping("/track/impression")
    @Operation(summary = "Đếm View (Impression)", description = "Gọi ngầm khi quảng cáo hiển thị thành công (Async).")
    public ResponseEntity<BaseResponse> trackImpression(@Valid @RequestBody AdTrackRequestDto request) {
        trackingService.trackImpressionAsync(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Impression tracked asynchronously")
                .build());
    }

    @PostMapping("/track/view-6s")
    @Operation(summary = "Đếm View 6s", description = "Gọi ngầm khi quảng cáo video phát được 6 giây (Async).")
    public ResponseEntity<BaseResponse> track6sView(@Valid @RequestBody AdTrackRequestDto request) {
        trackingService.track6sViewAsync(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("6s view tracked asynchronously")
                .build());
    }

    @PostMapping("/track/click")
    @Operation(summary = "Đếm Click", description = "Gọi ngầm khi người dùng click vào quảng cáo (Async).")
    public ResponseEntity<BaseResponse> trackClick(@Valid @RequestBody AdTrackRequestDto request) {
        trackingService.trackClickAsync(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Click tracked asynchronously")
                .build());
    }
}
