package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.filters.CreatorSettlementFilterRequestDto;
import com.talex.server.dtos.settlement.request.UpdateSettlementStatusRequestDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.creator.CreatorSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creator-settlement")
@RequiredArgsConstructor
@Tag(name = "Creator Settlement", description = "API chạy thử & tính toán quyết toán doanh thu hàng tháng cho Creator")
public class CreatorSettlementController {

    private final CreatorSettlementService creatorSettlementService;
    private final CreatorService creatorService;

    @GetMapping("/search")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tìm kiếm & Lọc danh sách quyết toán (Dành cho Admin)",
            description = "Lọc linh hoạt theo creatorMonthlySettlementId, settlementMonth, creatorId, danh sách statuses, phân trang và sắp xếp theo grossAmount, netPayoutAmount, status."
    )
    public ResponseEntity<BaseResponse> searchSettlements(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<CreatorSettlementResponseDto> pageResponse = creatorSettlementService
                .filterSettlements(CreatorSettlementFilterRequestDto.builder()
                        .criteria(criteria)
                        .statuses(statuses == null ? new String[0] : statuses)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/own")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Tìm kiếm & Lọc danh sách quyết toán (Dành cho Admin)",
            description = "Lọc linh hoạt theo creatorMonthlySettlementId, settlementMonth, creatorId, danh sách statuses, phân trang và sắp xếp theo grossAmount, netPayoutAmount, status."
    )
    public ResponseEntity<BaseResponse> searchOwnSettlements(
            @CurrentAccountId UUID accountId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        String creatorId =  creatorService.getIdByAccountId(accountId);
        criteria.put("creatorId", creatorId);
        BasePageResponse<CreatorSettlementResponseDto> pageResponse = creatorSettlementService
                .filterSettlements(CreatorSettlementFilterRequestDto.builder()
                        .criteria(criteria)
                        .statuses(statuses == null ? new String[0] : statuses)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{settlementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    @Operation(summary = "Lấy chi tiết bản ghi quyết toán theo ID")
    public ResponseEntity<BaseResponse> getById(@PathVariable String settlementId) {
        CreatorSettlementDetailResponseDto response = creatorSettlementService.getSettlementById(settlementId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PatchMapping("/{settlementId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cập nhật trạng thái quyết toán (Dành cho Admin)",
            description = "Cho phép Admin chuyển đổi giữa các trạng thái APPROVED, UNDER_REVIEW, FORFEITED,... Yêu cầu có note/lý do đối với UNDER_REVIEW và FORFEITED."
    )
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable String settlementId,
            @Valid @RequestBody UpdateSettlementStatusRequestDto request
    ) {
        CreatorSettlementDetailResponseDto response = creatorSettlementService
                .updateSettlementStatus(settlementId, request);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật trạng thái quyết toán thành công")
                .data(response)
                .build());
    }

    @PostMapping("/demo-process")
    @Operation(
            summary = "Chạy thử (Demo) quy trình quyết toán hàng tháng cho Creator",
            description = "Tính toán gom sổ cái, phạt, thuế và số tiền Net Payout cho tất cả Creator mà KHÔNG lưu CSDL (isDemo = true)."
    )
    public ResponseEntity<BaseResponse> demoProcessSettlement(
            @RequestParam(value = "isDemo", defaultValue = "true") Boolean isDemo,
            @RequestParam(value = "targetMonth", required = false) String targetMonth
    ) {
        List<CreatorMonthlySettlement> results;

        if (targetMonth != null && !targetMonth.isBlank()) {
            results = creatorSettlementService.processMonthlySettlement(isDemo, targetMonth);
        } else {
            results = creatorSettlementService.processMonthlySettlement(isDemo);
        }

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Chạy quyết toán thành công (isDemo: " + isDemo + ")")
                .data(isDemo ? results : null)
                .build());
    }
}