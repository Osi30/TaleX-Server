package com.talex.server.controllers.coin;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.coin.CoinTransactionResponseDto;
import com.talex.server.dtos.responses.coin.CoinWalletResponseDto;
import com.talex.server.services.coin.ICoinWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isAuthenticated()")
@Tag(name = "User - Coin Wallet", description = "API quản lý ví và lịch sử giao dịch Coin")
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
    @Operation(
            summary = "Lấy thông tin ví Coin",
            description = "Trả về số dư hiện tại, tổng Coin đã nhận và tổng Coin đã sử dụng của người dùng đang đăng nhập."
    )
    public ResponseEntity<BaseResponse> getMyWallet(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId
    ) {
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
    @Operation(
            summary = "Lấy lịch sử giao dịch Coin",
            description = "Trả về lịch sử cộng/trừ Coin theo thứ tự mới nhất, có phân trang. Tham số page bắt đầu từ 1 và size là số bản ghi trên mỗi trang."
    )
    public ResponseEntity<BaseResponse> getTransactionHistory(
            @Parameter(hidden = true) @CurrentAccountId UUID accountId,
            @Parameter(description = "Số trang cần lấy, bắt đầu từ 1", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số giao dịch trên mỗi trang", example = "10")
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
