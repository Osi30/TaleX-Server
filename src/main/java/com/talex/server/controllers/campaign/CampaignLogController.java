package com.talex.server.controllers.campaign;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignLogRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignLogResponseDto;
import com.talex.server.services.campaign.ICampaignLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/campaign-logs")
@RequiredArgsConstructor
@Tag(name = "Campaign Logs", description = "API quản lý lịch sử và nhật ký sự kiện chiến dịch")
public class CampaignLogController {
    private final ICampaignLogService campaignLogService;

    @PostMapping
    @Operation(summary = "Tạo nhật ký chiến dịch", description = "Tạo một bản ghi nhật ký cho chiến dịch và tài khoản liên quan.")
    public ResponseEntity<BaseResponse> create(@RequestBody CampaignLogRequestDto request) {
        CampaignLogResponseDto response = campaignLogService.createCampaignLog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Campaign log created")
                        .data(response)
                        .build());
    }

    @GetMapping
    @Operation(summary = "Lọc nhật ký chiến dịch", description = "Lấy danh sách nhật ký chiến dịch theo điều kiện lọc và phân trang.")
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) String[] eventTypes,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        BasePageResponse<CampaignLogResponseDto> pageResponse = campaignLogService.filterCampaignLogs(
                eventTypes, criteria, sortBy, sortDirection, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{campaignLogId}")
    @Operation(summary = "Lấy nhật ký theo ID", description = "Trả về chi tiết nhật ký chiến dịch theo id.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String campaignLogId) {
        CampaignLogResponseDto response = campaignLogService.getCampaignLogById(campaignLogId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @DeleteMapping("/{campaignLogId}")
    @Operation(summary = "Xóa nhật ký chiến dịch", description = "Xóa bản ghi nhật ký chiến dịch theo id.")
    public ResponseEntity<BaseResponse> delete(@PathVariable String campaignLogId) {
        campaignLogService.deleteCampaignLog(campaignLogId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Campaign log deleted")
                .data(null)
                .build());
    }
}
