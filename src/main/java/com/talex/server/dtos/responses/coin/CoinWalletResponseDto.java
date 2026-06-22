package com.talex.server.dtos.responses.coin;

import com.talex.server.entities.coin.CoinWallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO hiển thị thông tin ví Coin cho phía client.
 * Không expose các trường audit (createdAt, updatedBy...) của entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinWalletResponseDto {

    /** Số dư hiện tại. */
    private BigDecimal balance;

    /** Tổng coin đã nhận từ trước đến nay. */
    private BigDecimal totalEarned;

    /** Tổng coin đã chi từ trước đến nay. */
    private BigDecimal totalSpent;

    /**
     * Factory method — map trực tiếp từ entity, tránh phụ thuộc vào MapStruct
     * cho một DTO nhỏ thuần túy chỉ đọc.
     *
     * @param wallet Entity CoinWallet (có thể là ví "ảo" balance=0 nếu chưa có giao dịch)
     * @return DTO an toàn để trả về client
     */
    public static CoinWalletResponseDto fromEntity(CoinWallet wallet) {
        return CoinWalletResponseDto.builder()
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalSpent(wallet.getTotalSpent())
                .build();
    }
}
