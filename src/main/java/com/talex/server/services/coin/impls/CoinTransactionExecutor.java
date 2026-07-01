package com.talex.server.services.coin.impls;

import com.talex.server.entities.coin.CoinTransaction;
import com.talex.server.entities.coin.CoinWallet;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.coin.CoinTransactionType;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.CoinTransactionRepository;
import com.talex.server.repositories.coin.CoinWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Component chuyên biệt thực thi các thao tác ghi DB cho module Coin.
 *
 * <h3>Tại sao tách thành class riêng?</h3>
 * <p>
 * Spring {@code @Transactional} hoạt động thông qua AOP Proxy. Nếu một method
 * không có {@code @Transactional} trong cùng class gọi một method có
 * {@code @Transactional}, lời gọi đi qua {@code this} (không qua proxy)
 * → annotation bị bỏ qua hoàn toàn (Self-Invocation problem).
 * </p>
 * <p>
 * Giải pháp: Tách logic DB ra {@code CoinTransactionExecutor} (@Component riêng).
 * Khi {@link CoinWalletServiceImpl} inject và gọi các method ở đây,
 * lời gọi đi qua Spring Proxy → {@code @Transactional} hoạt động đúng.
 * </p>
 *
 * <h3>Luồng:</h3>
 * <pre>
 * CoinWalletServiceImpl.creditCoin()
 *   → [Redis Lock acquired]
 *   → coinTransactionExecutor.executeCredit()   ← đi qua Spring AOP Proxy
 *       → @Transactional bắt đầu
 *       → findByAccountId / Lazy Init
 *       → coinWalletRepository.save(wallet)
 *       → coinTransactionRepository.save(transaction)
 *       → @Transactional commit
 *   → [Redis Lock released in finally{}]
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoinTransactionExecutor {

    private final CoinWalletRepository coinWalletRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // CREDIT — Cộng coin vào ví
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Thực thi giao dịch CREDIT trong một DB transaction nguyên tử.
     * Tự động tạo ví nếu user chưa có (Lazy Init).
     *
     * @param accountId     ID tài khoản nhận coin
     * @param amount        Số coin cộng vào (đã được validate > 0 ở tầng gọi)
     * @param referenceType Phân loại nguồn gốc giao dịch (ví dụ: "DAILY_CHECK_IN")
     * @param referenceId   ID bản ghi nguồn gốc (polymorphic, lưu dạng String)
     * @param description   Mô tả thân thiện với người dùng
     * @return Ví sau khi đã cập nhật số dư
     */
    @Transactional(rollbackFor = Exception.class)
    public CoinWallet executeCredit(UUID accountId, BigDecimal amount,
                                    CoinReferenceType referenceType, String referenceId, String description) {
        // Lazy Init: tạo ví nếu user chưa có bất kỳ giao dịch nào trước đây
        CoinWallet wallet = coinWalletRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    log.info("Lazy Init: Creating new CoinWallet for account [{}]", accountId);
                    CoinWallet newWallet = CoinWallet.builder()
                            .accountId(accountId)
                            .balance(BigDecimal.ZERO)
                            .totalEarned(BigDecimal.ZERO)
                            .totalSpent(BigDecimal.ZERO)
                            .build();
                    // Audit: ví được tạo bởi hệ thống khi user nhận coin lần đầu
                    newWallet.markCreatedBy("SYSTEM");
                    return coinWalletRepository.save(newWallet);
                });

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.add(amount);

        // 1. Cập nhật ví (audit: ghi lại loại giao dịch đã kích hoạt lần cập nhật này)
        wallet.setBalance(balanceAfter);
        wallet.setTotalEarned(wallet.getTotalEarned().add(amount));
//        wallet.markUpdatedBy(referenceType);
        CoinWallet savedWallet = coinWalletRepository.save(wallet);

        // 2. Ghi lịch sử giao dịch (immutable append-only log)
        coinTransactionRepository.save(
                CoinTransaction.builder()
                        .walletId(savedWallet.getWalletId())
                        .amount(amount)
                        .transactionType(CoinTransactionType.CREDIT)
                        .balanceBefore(balanceBefore)
                        .balanceAfter(balanceAfter)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .description(description)
                        .build()
        );

        log.info("CREDIT {} coins | account=[{}] | {} -> {}", amount, accountId, balanceBefore, balanceAfter);
        return savedWallet;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEBIT — Trừ coin khỏi ví
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Thực thi giao dịch DEBIT trong một DB transaction nguyên tử.
     * Kiểm tra số dư trước, ném {@link CoinException} nếu không đủ.
     *
     * @param accountId     ID tài khoản bị trừ coin
     * @param amount        Số coin trừ đi (đã được validate > 0 ở tầng gọi)
     * @param referenceType Phân loại nguồn gốc giao dịch (ví dụ: "PURCHASE")
     * @param referenceId   ID bản ghi nguồn gốc (polymorphic, lưu dạng String)
     * @param description   Mô tả thân thiện với người dùng
     * @return Ví sau khi đã cập nhật số dư
     */
    @Transactional(rollbackFor = Exception.class)
    public CoinWallet executeDebit(UUID accountId, BigDecimal amount,
                                   CoinReferenceType referenceType, String referenceId, String description) {
        CoinWallet wallet = coinWalletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new CoinException(CoinErrorCode.WALLET_NOT_FOUND));

        BigDecimal balanceBefore = wallet.getBalance();

        // Kiểm tra số dư đủ không — fail fast trước khi ghi bất cứ gì
        if (balanceBefore.compareTo(amount) < 0) {
            log.warn("Insufficient balance | account=[{}] | required={} available={}",
                    accountId, amount, balanceBefore);
            throw new CoinException(CoinErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // 1. Cập nhật ví (audit: ghi lại loại giao dịch đã kích hoạt lần cập nhật này)
        wallet.setBalance(balanceAfter);
        wallet.setTotalSpent(wallet.getTotalSpent().add(amount));
//        wallet.markUpdatedBy(referenceType);
        CoinWallet savedWallet = coinWalletRepository.save(wallet);

        // 2. Ghi lịch sử giao dịch (immutable append-only log)
        coinTransactionRepository.save(
                CoinTransaction.builder()
                        .walletId(savedWallet.getWalletId())
                        .amount(amount)
                        .transactionType(CoinTransactionType.DEBIT)
                        .balanceBefore(balanceBefore)
                        .balanceAfter(balanceAfter)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .description(description)
                        .build()
        );

        log.info("DEBIT {} coins | account=[{}] | {} -> {}", amount, accountId, balanceBefore, balanceAfter);
        return savedWallet;
    }
}
