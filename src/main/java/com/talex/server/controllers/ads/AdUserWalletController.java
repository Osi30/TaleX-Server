package com.talex.server.controllers.ads;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.ads.AdTopupRequestDto;
import com.talex.server.services.ads.IAdWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads/wallet")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Ad Wallet", description = "API quản lý ví tiền quảng cáo của User")
public class AdUserWalletController {

    private final IAdWalletService walletService;

    @GetMapping("/balance")
    @Operation(summary = "Lấy số dư ví quảng cáo", description = "Lấy thông tin profile quảng cáo và số dư ví hiện tại.")
    public ResponseEntity<BaseResponse> getBalance(@CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(walletService.getOrCreateProfile(accountId))
                .build());
    }

    @PostMapping("/topup")
    @Operation(summary = "Nạp tiền vào ví (Mockup)", description = "Chỉ dùng để test nạp tiền trực tiếp vào ví quảng cáo.")
    public ResponseEntity<BaseResponse> topup(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody AdTopupRequestDto request) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Topup successful")
                .data(walletService.topupWallet(accountId, request))
                .build());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lấy lịch sử giao dịch", description = "Lấy danh sách lịch sử nạp/trừ tiền của ví.")
    public ResponseEntity<BaseResponse> getWalletTransactions(@CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(walletService.getWalletTransactions(accountId))
                .build());
    }
}
