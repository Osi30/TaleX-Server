package com.talex.server.dtos.responses.coin;

import com.talex.server.entities.coin.CoinTransaction;
import com.talex.server.enums.coin.CoinReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO hiển thị một bản ghi giao dịch Coin cho phía client.
 * Không expose walletId (internal) hay các trường audit không cần thiết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinTransactionResponseDto {

    private UUID transactionId;

    /** Số coin thay đổi trong giao dịch (luôn dương; chiều xác định bởi transactionType). */
    private BigDecimal amount;

    /** CREDIT hoặc DEBIT — trả về String để client không phụ thuộc enum nội bộ. */
    private String transactionType;

    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    /** Phân loại nguồn gốc (ví dụ: "DAILY_CHECK_IN", "PURCHASE"). */
    private CoinReferenceType referenceType;

    /** Mô tả thân thiện với người dùng. */
    private String description;

    /** Thời điểm giao dịch xảy ra. */
    private LocalDateTime changedAt;

    /**
     * Factory method — map trực tiếp từ entity CoinTransaction.
     *
     * @param entity Bản ghi giao dịch từ DB
     * @return DTO an toàn để trả về client
     */
    public static CoinTransactionResponseDto fromEntity(CoinTransaction entity) {
        return CoinTransactionResponseDto.builder()
                .transactionId(entity.getTransactionId())
                .amount(entity.getAmount())
                .transactionType(entity.getTransactionType() != null
                        ? entity.getTransactionType().name()
                        : null)
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .referenceType(entity.getReferenceType())
                .description(entity.getDescription())
                .changedAt(entity.getChangedAt())
                .build();
    }
}
