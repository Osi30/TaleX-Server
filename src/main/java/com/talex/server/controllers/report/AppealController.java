package com.talex.server.controllers.report;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.annotations.CurrentRole;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.report.request.AppealProcessRequestDto;
import com.talex.server.dtos.report.request.AppealRequestDto;
import com.talex.server.dtos.report.response.AppealResponseDto;
import com.talex.server.services.report.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
@Tag(name = "Appeals", description = "API xử lý khiếu nại hình phạt vi phạm (Gỡ gậy)")
public class AppealController {

    private final AppealService appealService;

    @PostMapping("/penalties/{penaltyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Tạo đơn khiếu nại hình phạt",
            description = "Người dùng gửi đơn khiếu nại cho một hình phạt (gậy) trong vòng 7 ngày kể từ khi bị phạt."
    )
    public ResponseEntity<BaseResponse> createAppeal(
            @PathVariable String penaltyId,
            @CurrentAccountId UUID accountId,
            @CurrentRole String role,
            @Valid @RequestBody AppealRequestDto request
    ) {
        AppealResponseDto response = appealService
                .createAppeal(accountId.toString(), role, penaltyId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Đơn khiếu nại đã được gửi thành công và đang chờ xét duyệt")
                        .data(response)
                        .build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lọc danh sách khiếu nại (Admin)",
            description = "Tìm kiếm và phân trang các đơn khiếu nại theo trạng thái, người khiếu nại,..."
    )
    public ResponseEntity<BaseResponse> filterAppeals(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<AppealResponseDto> pageResponse = appealService.filterAppeals(
                BaseFilterRequestDto.builder()
                        .criteria(criteria)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/own")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lọc danh sách khiếu nại của tôi",
            description = "Tìm kiếm và phân trang các đơn khiếu nại theo trạng thái, người khiếu nại"
    )
    public ResponseEntity<BaseResponse> getMyAppeals(
            @CurrentAccountId UUID accountId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<AppealResponseDto> pageResponse = appealService.filterAppeals(
                accountId.toString(),
                BaseFilterRequestDto.builder()
                        .criteria(criteria)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @PutMapping("/{appealId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xét duyệt khiếu nại (Admin)",
            description = "Admin duyệt đơn khiếu nại. Nếu chấp nhận, hình phạt sẽ tự động bị huỷ (revoke)."
    )
    public ResponseEntity<BaseResponse> processAppeal(
            @PathVariable String appealId,
            @CurrentAccountId UUID accountId,
            @CurrentRole String role,
            @Valid @RequestBody AppealProcessRequestDto request
    ) {
        AppealResponseDto response = appealService
                .processAppeal(accountId.toString(), role, appealId, request);

        String message = request.getIsApproved() ? "Đã chấp nhận khiếu nại và gỡ bỏ hình phạt" : "Đã từ chối đơn khiếu nại";

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message(message)
                .data(response)
                .build());
    }

    @GetMapping("/penalties/{penaltyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy chi tiết khiếu nại theo Penalty ID",
            description = "Tra cứu thông tin chi tiết đơn khiếu nại dựa trên ID của hình phạt (Penalty ID)."
    )
    public ResponseEntity<BaseResponse> getAppealByPenaltyId(@PathVariable String penaltyId) {
        AppealResponseDto response = appealService.getAppealByPenaltyId(penaltyId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }
}