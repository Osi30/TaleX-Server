package com.talex.server.controllers.campaign;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.services.campaign.ICampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "API quản lý chiến dịch quảng cáo và hành trình marketing")
public class CampaignController {
    private final ICampaignService campaignService;

    @PostMapping
    @Operation(summary = "Tạo chiến dịch mới", description = "Tạo một chiến dịch quảng cáo mới cho creator và tài khoản.")
    public ResponseEntity<BaseResponse> create(@RequestBody CampaignRequestDto request) {
        CampaignResponseDto response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Campaign created")
                        .data(response)
                        .build());
    }

    @GetMapping
    @Operation(summary = "Lọc danh sách chiến dịch", description = "Lấy danh sách các chiến dịch theo điều kiện lọc, trạng thái và phân trang.")
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        BasePageResponse<CampaignResponseDto> pageResponse = campaignService.filterCampaigns(
                statuses, types, criteria, sortBy, sortDirection, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Lấy chiến dịch theo ID", description = "Trả về chi tiết chiến dịch theo id.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String campaignId) {
        CampaignResponseDto response = campaignService.getCampaignById(campaignId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{campaignId}")
    @Operation(summary = "Cập nhật chiến dịch", description = "Cập nhật thông tin chiến dịch theo id.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String campaignId,
            @RequestBody CampaignRequestDto request) {
        CampaignResponseDto response = campaignService.updateCampaign(campaignId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign updated")
                .data(response)
                .build());
    }

    @DeleteMapping("/{campaignId}")
    @Operation(summary = "Xóa chiến dịch", description = "Xóa chiến dịch theo id.")
    public ResponseEntity<BaseResponse> delete(@PathVariable String campaignId) {
        campaignService.deleteCampaign(campaignId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign deleted")
                .data(null)
                .build());
    }
}
