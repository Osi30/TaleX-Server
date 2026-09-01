package com.talex.server.controllers.report;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.annotations.CurrentRole;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.report.request.TicketProcessRequestDto;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.dtos.report.response.TicketResponseDto;
import com.talex.server.services.report.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation/tickets")
@RequiredArgsConstructor
@Tag(name = "Moderation Tickets", description = "API dành cho Staff/Admin để quản lý và xử lý Ticket kiểm duyệt")
public class ModerationTicketController {

    private final ModerationService moderationService;

    @GetMapping("/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin chi tiết Ticket kiểm duyệt",
            description = "Lấy chi tiết một Ticket kiểm duyệt theo ticketId bao gồm danh sách các báo cáo liên quan."
    )
    public ResponseEntity<BaseResponse> getTicketById(
            @PathVariable String ticketId
    ) {
        TicketResponseDto response = moderationService.getTicketById(ticketId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Lọc danh sách Ticket kiểm duyệt",
            description = "Tìm kiếm và phân trang danh sách ticket kiểm duyệt dựa trên độ ưu tiên, trạng thái, loại đối tượng."
    )
    public ResponseEntity<BaseResponse> filterTickets(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<TicketResponseDto> pageResponse = moderationService.filterTickets(
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

    @PutMapping("/{ticketId}/assign")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Gán Ticket cho Staff xử lý",
            description = "Gán quyền xử lý ticket kiểm duyệt cho một nhân viên cụ thể."
    )
    public ResponseEntity<BaseResponse> assignTicket(
            @PathVariable String ticketId,
            @CurrentAccountId UUID accountId,
            @CurrentRole String role
    ) {
        TicketResponseDto response = moderationService.assignTicketToStaff(
                ticketId,
                role,
                accountId.toString()
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đã nhận xử lý Ticket thành công")
                .data(response)
                .build());
    }

    @PostMapping("/{ticketId}/process")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(
            summary = "Xử lý Ticket kiểm duyệt",
            description = "Đưa ra quyết định phạt (ban hành gậy vi phạm) hoặc bác bỏ (dismiss) các báo cáo liên quan."
    )
    public ResponseEntity<BaseResponse> processTicket(
            @PathVariable String ticketId,
            @CurrentAccountId UUID accountId,
            @CurrentRole String role,
            @Valid @RequestBody TicketProcessRequestDto request
    ) {
        PenaltyResponseDto penaltyResponse = moderationService.processTicket(
                ticketId, accountId.toString(), role, request
        );

        String message = request.getIsApproved() ? "Xử lý vi phạm và ban hành gậy thành công" : "Đã bác bỏ ticket báo cáo";

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message(message)
                .data(penaltyResponse)
                .build());
    }
}