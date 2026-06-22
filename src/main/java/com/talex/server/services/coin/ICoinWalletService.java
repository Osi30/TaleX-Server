package com.talex.server.services.coin;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.coin.CoinTransactionResponseDto;
import com.talex.server.entities.coin.CoinWallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface định nghĩa Core Ledger operations cho module Coin.
 * <p>
 * Tầng Controller chỉ được inject interface này, KHÔNG inject Repository.
 * accountId luôn đến từ {@code @CurrentAccountId} ở tầng Controller.
 * </p>
 */
public interface ICoinWalletService {

    /**
     * Cộng coin vào ví (CREDIT).
     * <p>
     * Luồng bắt buộc: Distributed Lock → @Transactional → Update Wallet → Insert Transaction.
     * Tự động tạo ví nếu user chưa có (Lazy Init).
     * </p>
     *
     * @param accountId     ID tài khoản nhận coin
     * @param amount        Số coin muốn cộng (phải > 0)
     * @param referenceType Loại nguồn gốc (ví dụ: "DAILY_CHECK_IN")
     * @param referenceId   ID của bản ghi nguồn gốc (ví dụ: checkinId.toString())
     * @param description   Mô tả giao dịch thân thiện với người dùng
     * @return Ví sau khi đã cộng coin
     */
    CoinWallet creditCoin(UUID accountId, BigDecimal amount,
                          String referenceType, String referenceId, String description);

    /**
     * Trừ coin khỏi ví (DEBIT).
     * <p>
     * Luồng bắt buộc: Distributed Lock → @Transactional → Kiểm tra số dư → Update Wallet → Insert Transaction.
     * Ném {@link com.talex.server.exceptions.details.CoinException} với code INSUFFICIENT_BALANCE
     * nếu số dư không đủ.
     * </p>
     *
     * @param accountId     ID tài khoản bị trừ coin
     * @param amount        Số coin muốn trừ (phải > 0)
     * @param referenceType Loại nguồn gốc (ví dụ: "PURCHASE")
     * @param referenceId   ID của bản ghi nguồn gốc
     * @param description   Mô tả giao dịch thân thiện với người dùng
     * @return Ví sau khi đã trừ coin
     */
    CoinWallet debitCoin(UUID accountId, BigDecimal amount,
                         String referenceType, String referenceId, String description);

    /**
     * Lấy thông tin ví hiện tại của tài khoản.
     * Trả về ví với balance = 0 nếu user chưa có ví (không tạo mới).
     *
     * @param accountId ID tài khoản cần xem ví
     * @return Ví Coin của tài khoản
     */
    CoinWallet getMyWallet(UUID accountId);

    /**
     * Lấy lịch sử giao dịch coin của tài khoản theo trang.
     * <p>
     * Convention: {@code page} nhận từ FE là 1-based (trang đầu = 1).
     * Impl phải chuyển sang 0-based khi gọi Spring Data JPA.
     * Nếu user chưa có ví, trả về trang rỗng (không throw exception).
     * </p>
     *
     * @param accountId ID tài khoản cần xem lịch sử
     * @param page      Số trang (1-based, bắt đầu từ 1)
     * @param size      Số bản ghi mỗi trang
     * @return Trang lịch sử giao dịch kèm metadata phân trang
     */
    BasePageResponse<CoinTransactionResponseDto> getTransactionHistory(UUID accountId, int page, int size);}
