package com.talex.server.services.coin.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.coin.CoinTransactionResponseDto;
import com.talex.server.entities.coin.CoinWallet;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.CoinTransactionRepository;
import com.talex.server.repositories.coin.CoinWalletRepository;
import com.talex.server.services.coin.ICoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Core Ledger Service — triển khai {@link ICoinWalletService}.
 *
 * <h3>Trách nhiệm của class này (Single Responsibility):</h3>
 * <ol>
 *   <li>Validate input (amount > 0)</li>
 *   <li>Acquire/Release Distributed Lock qua Redis</li>
 *   <li>Delegate logic DB sang {@link CoinTransactionExecutor}</li>
 * </ol>
 *
 * <h3>Tại sao KHÔNG có @Transactional ở đây?</h3>
 * <p>
 * Redis Lock phải được giải phóng SAU KHI DB transaction commit xong.
 * Nếu mở @Transactional ở đây bao ngoài Redis Lock, thứ tự sẽ là:
 * <br>→ Lock acquired → TX begin → ... → TX commit → <b>Lock released</b>  ✅
 * <br>Nhưng nếu @Transactional ở đây và bên trong cũng có @Transactional,
 * Spring sẽ reuse transaction ngoài → Executor không tạo transaction mới
 * → Hành vi không kiểm soát được (tùy Propagation).
 * <br><br>
 * Giải pháp hiện tại: Class này KHÔNG có @Transactional.
 * Executor có @Transactional của riêng nó, đảm bảo commit trước khi trả về.
 * Lock released trong finally{} SAU KHI Executor return → đúng thứ tự.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoinWalletServiceImpl implements ICoinWalletService {

    private static final String LOCK_PREFIX = "lock:coin_transaction:";
    private static final Duration LOCK_TTL   = Duration.ofSeconds(5);

    private final CoinWalletRepository coinWalletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final CoinTransactionExecutor coinTransactionExecutor;
    private final StringRedisTemplate stringRedisTemplate;

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     * <p>
     * Luồng: validate → acquire Redis lock → delegate đến {@link CoinTransactionExecutor#executeCredit}
     * (có @Transactional thật) → release lock trong finally{}.
     * </p>
     */
    @Override
    public CoinWallet creditCoin(UUID accountId, BigDecimal amount,
                                 String referenceType, String referenceId, String description) {
        validateAmount(amount);

        String lockKey = LOCK_PREFIX + accountId;
        acquireLock(lockKey);

        try {
            // Gọi qua Spring-managed bean → AOP Proxy hoạt động → @Transactional đúng
            return coinTransactionExecutor.executeCredit(accountId, amount, referenceType, referenceId, description);
        } finally {
            stringRedisTemplate.delete(lockKey);
            log.debug("Released coin lock [{}]", lockKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng: validate → acquire Redis lock → delegate đến {@link CoinTransactionExecutor#executeDebit}
     * (có @Transactional thật) → release lock trong finally{}.
     * </p>
     */
    @Override
    public CoinWallet debitCoin(UUID accountId, BigDecimal amount,
                                String referenceType, String referenceId, String description) {
        validateAmount(amount);

        String lockKey = LOCK_PREFIX + accountId;
        acquireLock(lockKey);

        try {
            // Gọi qua Spring-managed bean → AOP Proxy hoạt động → @Transactional đúng
            return coinTransactionExecutor.executeDebit(accountId, amount, referenceType, referenceId, description);
        } finally {
            stringRedisTemplate.delete(lockKey);
            log.debug("Released coin lock [{}]", lockKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Read-only, không cần Lock. Trả về ví "ảo" balance=0 nếu user chưa từng có giao dịch
     * (không INSERT record để tránh phình bảng).
     * </p>
     */
    @Override
    public CoinWallet getMyWallet(UUID accountId) {
        return coinWalletRepository.findByAccountId(accountId)
                .orElseGet(() -> CoinWallet.builder()
                        .accountId(accountId)
                        .balance(BigDecimal.ZERO)
                        .totalEarned(BigDecimal.ZERO)
                        .totalSpent(BigDecimal.ZERO)
                        .build());
    }

    /**
     * {@inheritDoc}
     * <p>
     * FE gửi page 1-based → chuyển sang 0-based bằng {@code page - 1} trước khi gọi JPA.
     * Nếu user chưa có ví (chưa từng có giao dịch), trả về trang rỗng thay vì throw exception.
     * </p>
     */
    @Override
    public BasePageResponse<CoinTransactionResponseDto> getTransactionHistory(UUID accountId, int page, int size) {
        // Dùng if/else thay vì Optional.map().orElseGet() để tránh lỗi Java type inference
        // với generic @SuperBuilder trong lambda ("Bad return type in lambda expression")
        java.util.Optional<CoinWallet> walletOpt = coinWalletRepository.findByAccountId(accountId);

        if (walletOpt.isEmpty()) {
            // User chưa có ví → trả về trang rỗng, không throw exception
            return BasePageResponse.<CoinTransactionResponseDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(page)
                    .pageSize(size)
                    .totalElements(0L)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .build();
        }

        PageRequest pageable = PageRequest.of(page - 1, size);  // FE 1-based → JPA 0-based
        Page<CoinTransactionResponseDto> result = coinTransactionRepository
                .findByWalletIdOrderByChangedAtDesc(walletOpt.get().getWalletId(), pageable)
                .map(CoinTransactionResponseDto::fromEntity);

        return BasePageResponse.<CoinTransactionResponseDto>builder()
                .content(result.getContent())
                .pageNumber(result.getNumber() + 1)  // JPA 0-based → FE 1-based
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .isFirst(result.isFirst())
                .isLast(result.isLast())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Thử lấy Distributed Lock bằng Redis SET NX (set-if-absent).
     * Nếu lock đã tồn tại → giao dịch khác đang xử lý cho account này → 429.
     */
    private void acquireLock(String lockKey) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("Coin transaction already in progress: {}", lockKey);
            throw new CoinException(CoinErrorCode.COIN_PROCESSING);
        }

        log.debug("Acquired coin lock [{}]", lockKey);
    }

    /**
     * Fail-fast validation: đảm bảo amount hợp lệ trước khi tốn chi phí lấy lock.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoinException(CoinErrorCode.INVALID_AMOUNT);
        }
    }
}
