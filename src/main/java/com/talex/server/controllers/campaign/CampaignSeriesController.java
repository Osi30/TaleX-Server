package com.talex.server.controllers.campaign;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.analytic.CampaignSeriesLogResponseDto;
import com.talex.server.dtos.campaign.response.CampaignSeriesResponseDto;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.services.campaign.CampaignSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/campaign-series")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Campaign Series", description = "API quản lý danh sách và trạng thái của Campaign Series")
public class CampaignSeriesController {
    private final CampaignSeriesService campaignSeriesService;

    @GetMapping("/campaign/{campaignId}")
    @Operation(
            summary = "Lấy danh sách Campaign Series theo Campaign ID",
            description = "Trả về danh sách các Campaign Series thuộc về một Campaign cụ thể dựa vào campaignId."
    )
    public ResponseEntity<BaseResponse> getByCampaignId(
            @PathVariable String campaignId
    ) {
        List<CampaignSeriesResponseDto> response = campaignSeriesService.getByCampaignId(campaignId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/{campaignSeriesId}/logs")
    @Operation(
            summary = "Lấy danh sách log của Campaign Series theo khoảng thời gian",
            description = "Lấy ra danh sách các log thống kê theo giờ (hourBucket) trong khoảng từ startTime đến endTime dựa vào campaignSeriesId."
    )
    public ResponseEntity<BaseResponse> getLogs(
            @PathVariable String campaignSeriesId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<CampaignSeriesLogResponseDto> response = campaignSeriesService.getLogs(campaignSeriesId, startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Cập nhật trạng thái Campaign Series",
            description = "Chỉ cho phép chuyển đổi trạng thái hai chiều giữa RUNNING và PAUSED (RUNNING -> PAUSED hoặc PAUSED -> RUNNING)."
    )
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable String id,
            @RequestBody CampaignStatus newStatus
    ) {
        CampaignSeriesResponseDto response = campaignSeriesService.updateStatus(id, newStatus);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign series status updated")
                .data(response)
                .build());
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Hủy Campaign Series",
            description = "Chuyển trạng thái sang CANCELLED. Chỉ thực hiện được khi trạng thái hiện tại là RUNNING hoặc PAUSED."
    )
    public ResponseEntity<BaseResponse> cancelCampaignSeries(@PathVariable String id) {
        CampaignSeriesResponseDto response = campaignSeriesService.cancelCampaignSeries(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign series cancelled")
                .data(response)
                .build());
    }
}