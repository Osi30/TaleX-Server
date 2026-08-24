package com.talex.server.controllers.admin;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.payment.AdminOrderDetailDto;
import com.talex.server.dtos.responses.payment.AdminOrderListItemDto;
import com.talex.server.dtos.responses.payment.AdminOrderStatsDto;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.services.payment.AdminOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Admin-only order lookup: search, detail, and stats. Not scoped by account like
 * {@code OrderController} — only ADMIN can access.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Quản lý đơn hàng phía Admin — tra cứu, thống kê")
public class AdminOrderController {

    private final AdminOrderQueryService adminOrderQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách đơn hàng (tìm kiếm/lọc)",
            description = "Lọc theo trạng thái, loại đơn, khoảng ngày tạo, từ khóa (mã đơn/paymentCode/username/email/họ tên người mua — họ tên tìm không phân biệt dấu).")
    public ResponseEntity<BaseResponse> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        BasePageResponse<AdminOrderListItemDto> response = adminOrderQueryService.search(
                status, itemType, createdAtFrom, createdAtTo, keyword, page, pageSize);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết 1 đơn hàng")
    public ResponseEntity<BaseResponse> getDetail(@PathVariable String orderId) {
        AdminOrderDetailDto response = adminOrderQueryService.getDetail(orderId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê tổng quan đơn hàng",
            description = "Đếm đơn theo trạng thái, doanh thu theo loại đơn (SUBSCRIPTION/EPISODE/COMBO/ENGAGEMENT), tỷ lệ hủy/hết hạn trong khoảng thời gian.")
    public ResponseEntity<BaseResponse> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        AdminOrderStatsDto response = adminOrderQueryService.getStats(from, to);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }
}
