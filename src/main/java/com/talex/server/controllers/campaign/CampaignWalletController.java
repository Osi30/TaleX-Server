package com.talex.server.controllers.campaign;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.campaign.response.CampaignWalletBalanceDto;
import com.talex.server.dtos.campaign.response.CampaignWalletTransactionDto;
import com.talex.server.services.campaign.CampaignWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaign-wallets")
@RequiredArgsConstructor
@Tag(name = "Campaign Wallet", description = "API quản lý và tra cứu ví Campaign Wallet")
public class CampaignWalletController {

    private final CampaignWalletService campaignWalletService;

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy số dư ví Campaign Wallet",
            description = "Trả về số dư ví. Nếu chưa tạo ví thì vẫn trả về HTTP 200 với data = null.")
    public ResponseEntity<BaseResponse> getBalance(@CurrentAccountId UUID accountId) {
        CampaignWalletBalanceDto response = campaignWalletService.getWalletBalanceDto(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lịch sử biến động ví Campaign Wallet",
            description = "Danh sách lịch sử biến động số dư ví của Creator đang đăng nhập.")
    public ResponseEntity<BaseResponse> getHistory(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<CampaignWalletTransactionDto> response = campaignWalletService
                .getWalletHistory(accountId, PageRequest.of(page - 1, pageSize));
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/{orderId}/wallet-transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy Wallet Transaction của Order",
            description = "Lấy vết giao dịch khấu trừ/hoàn tiền ví Campaign Wallet tương ứng với Order.")
    public ResponseEntity<BaseResponse> getWalletTransactionsByOrder(@PathVariable String orderId) {
        List<CampaignWalletTransactionDto> response = campaignWalletService.getTransactionsByOrderId(orderId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }
}