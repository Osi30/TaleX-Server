package com.talex.server.controllers.report;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.annotations.CurrentRole;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.services.report.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/penalties")
@RequiredArgsConstructor
@Tag(name = "Penalties", description = "API quản lý hình phạt/gậy vi phạm")
public class PenaltyController {

    private final PenaltyService penaltyService;

    @GetMapping("/my-penalties")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem lịch sử vi phạm của tôi", description = "Trả về danh sách các gậy/hình phạt mà tài khoản hiện tại đang chịu.")
    public ResponseEntity<BaseResponse> getMyPenalties(
            @CurrentAccountId UUID accountId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<PenaltyResponseDto> pageResponse = penaltyService.getMyPenalties(
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

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Tra cứu hình phạt (Staff/Admin)", description = "Lọc danh sách gậy vi phạm toàn hệ thống.")
    public ResponseEntity<BaseResponse> filterPenalties(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<PenaltyResponseDto> pageResponse = penaltyService.filterPenalties(
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

    @GetMapping("/{penaltyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết hình phạt", description = "Lấy chi tiết thông tin một hình phạt theo ID.")
    public ResponseEntity<BaseResponse> getById(@PathVariable String penaltyId) {
        PenaltyResponseDto response = penaltyService.getPenaltyById(penaltyId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PutMapping("/{penaltyId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gỡ gậy thủ công (Admin)", description = "Admin thực hiện thu hồi/gỡ bỏ hình phạt cho người dùng.")
    public ResponseEntity<BaseResponse> revokePenalty(
            @PathVariable String penaltyId,
            @RequestParam String reason,
            @CurrentAccountId UUID accountId,
            @CurrentRole String role
    ) {
        PenaltyResponseDto response = penaltyService
                .revokePenalty(penaltyId, accountId.toString(), role, reason);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Thu hồi hình phạt thành công")
                .data(response)
                .build());
    }
}