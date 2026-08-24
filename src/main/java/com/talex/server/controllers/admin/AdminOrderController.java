package com.talex.server.controllers.admin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.payment.OrderInterventionRequestDto;
import com.talex.server.dtos.responses.payment.AdminOrderDetailDto;
import com.talex.server.dtos.responses.payment.AdminOrderListItemDto;
import com.talex.server.dtos.responses.payment.AdminOrderStatsDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.services.payment.AdminOrderInterventionService;
import com.talex.server.services.payment.AdminOrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin-only order management: tra cứu, đối soát tiền thừa, thống kê, và can thiệp thủ công
 * (hủy / hoàn tất) cho đơn hàng bị kẹt. Khác {@code OrderController} — không scope theo account,
 * chỉ ADMIN mới truy cập được (không STAFF, vì 2 action ghi có ảnh hưởng tài chính trực tiếp).
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Quản lý đơn hàng phía Admin — tra cứu, đối soát tiền thừa, thống kê, can thiệp thủ công")
public class AdminOrderController {

    private final AdminOrderQueryService adminOrderQueryService;
    private final AdminOrderInterventionService adminOrderInterventionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách đơn hàng (tìm kiếm/lọc)",
            description = "Lọc theo trạng thái, loại đơn, khoảng ngày tạo, từ khóa (mã đơn/paymentCode/username/email người mua).")
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

    @GetMapping("/overpaid")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách đơn có tiền thừa",
            description = "Đơn có overpaidAmount > 0 (khách chuyển thừa qua SePay), để Admin đối soát hoàn tiền thủ công bên ngoài hệ thống.")
    public ResponseEntity<BaseResponse> listOverpaid(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        BasePageResponse<AdminOrderListItemDto> response = adminOrderQueryService.listOverpaid(page, pageSize);
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

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin hủy đơn thủ công",
            description = "Hủy đơn đang chờ thanh toán hoặc đã hết hạn, hoàn Coin/Campaign Wallet đã áp (nếu có). Bắt buộc nhập lý do, ghi audit log.")
    public ResponseEntity<BaseResponse> cancel(
            @PathVariable String orderId,
            @Valid @RequestBody OrderInterventionRequestDto request,
            @CurrentAccountId UUID adminId) {
        Order order = adminOrderInterventionService.cancelByAdmin(orderId, adminId, request.getReason());
        AdminOrderDetailDto response = adminOrderQueryService.toDetailDto(order);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Order cancelled by admin")
                .data(response)
                .build());
    }

    @PostMapping("/{orderId}/force-complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin đánh dấu hoàn tất thủ công",
            description = "Dùng khi tiền đã về nhưng webhook SePay không chạy được (đơn bị kẹt). Chạy đúng luồng hoàn tất/unlock nội dung như thanh toán thật. Bắt buộc nhập lý do, ghi audit log.")
    public ResponseEntity<BaseResponse> forceComplete(
            @PathVariable String orderId,
            @Valid @RequestBody OrderInterventionRequestDto request,
            @CurrentAccountId UUID adminId) {
        Order order = adminOrderInterventionService.forceCompleteByAdmin(orderId, adminId, request.getReason());
        AdminOrderDetailDto response = adminOrderQueryService.toDetailDto(order);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Order force-completed by admin")
                .data(response)
                .build());
    }
}
