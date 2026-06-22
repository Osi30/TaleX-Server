package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.coin.CoinTransactionResponseDto;
import com.talex.server.dtos.responses.coin.CoinWalletResponseDto;
import com.talex.server.services.coin.ICoinWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller quản lý thông tin Ví Coin.
 *
 * <p>Base path: {@code /api/v1/coins}</p>
 *
 * <ul>
 *   <li>{@code GET /wallet} — Xem số dư ví hiện tại</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/coins")
@RequiredArgsConstructor
public class CoinController {

    private final ICoinWalletService coinWalletService;

    /**
     * Lấy thông tin ví Coin của user đang đăng nhập.
     * Nếu user chưa có giao dịch nào, trả về ví ảo với balance = 0 (không tạo record DB).
     *
     * @param accountId ID tài khoản, lấy tự động từ JWT token qua {@code @CurrentAccountId}
     * @return Thông tin ví: balance, totalEarned, totalSpent
     */
    @GetMapping("/wallet")
    public ResponseEntity<BaseResponse> getMyWallet(@CurrentAccountId UUID accountId) {
        CoinWalletResponseDto dto = CoinWalletResponseDto.fromEntity(
                coinWalletService.getMyWallet(accountId)
        );

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy thông tin ví thành công")
                        .data(dto)
                        .build()
        );
    }

    /**
     * Lấy lịch sử giao dịch coin có phân trang.
     * <p>
     * FE gửi page 1-based (mặc định = 1). Backend xử lý chuyển đổi sang 0-based.
     * </p>
     *
     * @param accountId ID tài khoản, lấy tự động từ JWT token
     * @param page      Số trang (1-based, mặc định = 1)
     * @param size      Số bản ghi mỗi trang (mặc định = 10)
     * @return Danh sách giao dịch kèm metadata phân trang
     */
    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse> getTransactionHistory(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BasePageResponse<CoinTransactionResponseDto> history =
                coinWalletService.getTransactionHistory(accountId, page, size);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy lịch sử giao dịch thành công")
                        .data(history)
                        .build()
        );
    }
}