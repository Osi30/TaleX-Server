package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.response.PayoutAccountBalanceResponseDto;
import com.talex.server.services.payout.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts", description = "API quản lý và thực hiện lệnh chi hộ qua PayOS")
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo lô lệnh chi hàng loạt",
            description = "Gửi danh sách các lệnh chi tiền tới cổng thanh toán PayOS để tự động thực hiện chi hộ."
    )
    public ResponseEntity<BaseResponse> createBatchPayout(
            @Valid @RequestBody BatchPayoutRequestDto request
    ) {
        BatchPayoutDataResponseDto response = payoutService.createBatchPayout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Tạo lô lệnh chi thành công")
                        .data(response)
                        .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/balance")
    @Operation(
            summary = "Lấy dữ liệu số dư ví chi hộ",
            description = "Lấy dữ liệu số dư ví chi hộ hiện tại từ PayOS."
    )
    public ResponseEntity<PayoutAccountBalanceResponseDto> getAccountBalance() {
        PayoutAccountBalanceResponseDto balanceInfo = payoutService.getAccountBalance();
        return ResponseEntity.ok(balanceInfo);
    }
}