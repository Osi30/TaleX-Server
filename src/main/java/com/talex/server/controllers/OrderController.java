package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.payment.CreateContentOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateEngagementOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateOrderRequestDto;
import com.talex.server.dtos.responses.payment.OrderResponseDto;
import com.talex.server.services.payment.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "API tạo và tra cứu đơn hàng thanh toán qua SePay")
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo đơn hàng", description = "Tạo đơn hàng cho gói Premium và sinh mã QR SePay để thanh toán.")
    public ResponseEntity<BaseResponse> create(
            @Valid @RequestBody CreateOrderRequestDto request,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.createOrder(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Order created")
                        .data(response)
                        .build());
    }

    @PostMapping("/engagement")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo đơn hàng đẩy tương tác", description = "Tạo đơn hàng mua gói dịch vụ tương tác (Campaign) cho các tập phim/truyện đã chọn và sinh mã QR SePay.")
    public ResponseEntity<BaseResponse> createEngagementOrder(
            @Valid @RequestBody CreateEngagementOrderRequestDto request,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.createEngagementOrder(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Engagement order created")
                        .data(response)
                        .build());
    }

    @PostMapping("/content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo đơn hàng mua Episode/Combo", description = "Tạo đơn hàng mua lẻ tập phim/truyện hoặc combo và sinh mã QR SePay để thanh toán.")
    public ResponseEntity<BaseResponse> createContentOrder(
            @Valid @RequestBody CreateContentOrderRequestDto request,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.createContentOrder(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Content order created")
                        .data(response)
                        .build());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tra cứu đơn hàng", description = "Lấy trạng thái đơn hàng để FE polling sau khi quét QR.")
    public ResponseEntity<BaseResponse> get(
            @PathVariable String orderId,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.getOrder(orderId, accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy đơn hàng", description = "Hủy đơn hàng đang chờ thanh toán, hoàn lại Coin đã trừ (nếu có).")
    public ResponseEntity<BaseResponse> cancel(
            @PathVariable String orderId,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.cancelOrder(orderId, accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Order cancelled")
                .data(response)
                .build());
    }

    @PostMapping("/{orderId}/confirm-coin-payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác nhận thanh toán bằng Coin",
            description = "Hoàn tất đơn hàng khi Coin đã áp đủ trả hết (fiatAmount = 0). Yêu cầu user bấm xác nhận, không tự động.")
    public ResponseEntity<BaseResponse> confirmCoinPayment(
            @PathVariable String orderId,
            @CurrentAccountId UUID accountId) {
        OrderResponseDto response = orderService.confirmCoinPayment(orderId, accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Order completed via Coin")
                .data(response)
                .build());
    }
}
