package com.talex.server.services.coin.impls;

import com.talex.server.dtos.responses.coin.CoinEconomyConfigResponseDto;
import com.talex.server.dtos.responses.coin.DailyCheckInResponseDto;
import com.talex.server.dtos.responses.coin.DailyCheckInStatusDto;
import com.talex.server.entities.coin.DailyCheckIn;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.DailyCheckInRepository;
import com.talex.server.services.coin.ICoinEconomyConfigService;
import com.talex.server.services.coin.ICoinWalletService;
import com.talex.server.services.coin.IDailyCheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ Điểm danh hằng ngày.
 *
 * <h3>Cơ chế bảo vệ lồng nhau (2 lớp Lock):</h3>
 * <pre>
 * checkIn()
 *   → [Lock checkin: lock:checkin:{accountId}, TTL=10s]
 *   → self.executeCheckInTransaction()        ← đi qua Spring AOP Proxy (@Lazy self-inject)
 *       → @Transactional BEGIN
 *       → existsByAccountIdAndCheckInDate()   ← idempotency check
 *       → tính streak + reward
 *       → checkInRepository.save()
 *       → coinWalletService.creditCoin()
 *             → [Lock wallet: lock:coin_transaction:{accountId}, TTL=5s]
 *             → CoinTransactionExecutor.executeCredit()  ← @Transactional (REQUIRED, reuse outer TX)
 *             → [Unlock wallet]
 *       → @Transactional COMMIT
 *   → [Unlock checkin]
 * </pre>
 *
 * <h3>Tại sao dùng Self-Injection thay vì tách Component?</h3>
 * <p>
 * {@link CoinTransactionExecutor} cần inject 2 repositories → tách Component là hợp lý.
 * {@code executeCheckInTransaction} chỉ là 1 method của chính service này → self-inject
 * gọn hơn và không tạo thêm class dư thừa.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyCheckInServiceImpl implements IDailyCheckInService {

    private final DailyCheckInRepository checkInRepository;
    private final ICoinWalletService coinWalletService;
    private final ICoinEconomyConfigService configService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Self-inject để giải quyết Self-Invocation problem.
     * <p>
     * {@code @Lazy} bắt buộc để tránh vòng lặp circular dependency khi Spring
     * khởi tạo bean. Non-final + {@code @Autowired} vì Lombok {@code @RequiredArgsConstructor}
     * không xử lý được @Lazy trong constructor injection.
     * </p>
     */
    @Autowired
    @Lazy
    private IDailyCheckInService self;

    private static final String LOCK_PREFIX = "lock:checkin:";
    private static final Duration LOCK_TTL  = Duration.ofSeconds(10);

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public DailyCheckInStatusDto getCheckInStatus(UUID accountId) {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        boolean isCheckedIn = checkInRepository.existsByAccountIdAndCheckInDate(accountId, today);

        int currentStreak;
        if (isCheckedIn) {
            // Đã điểm danh hôm nay → lấy streak của hôm nay
            currentStreak = checkInRepository
                    .findByAccountIdAndCheckInDate(accountId, today)
                    .map(DailyCheckIn::getConsecutiveDays)
                    .orElse(1);
        } else {
            // Chưa điểm danh hôm nay → streak hiện tại = streak của hôm qua (0 nếu không có)
            currentStreak = checkInRepository
                    .findByAccountIdAndCheckInDate(accountId, yesterday)
                    .map(DailyCheckIn::getConsecutiveDays)
                    .orElse(0);
        }

        return DailyCheckInStatusDto.builder()
                .isCheckedInToday(isCheckedIn)
                .currentStreak(currentStreak)
                .build();
    }

    @Override
    public DailyCheckInResponseDto checkIn(UUID accountId) {
        String lockKey = LOCK_PREFIX + accountId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", LOCK_TTL);

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("Check-in already in progress for account [{}]", accountId);
            throw new CoinException(CoinErrorCode.COIN_PROCESSING, "Đang xử lý điểm danh, vui lòng thử lại sau");
        }

        try {
            // Gọi qua self (Spring-managed proxy) → @Transactional trên executeCheckInTransaction hoạt động đúng
            return self.executeCheckInTransaction(accountId);
        } finally {
            stringRedisTemplate.delete(lockKey);
            log.debug("Released check-in lock [{}]", lockKey);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSACTIONAL EXECUTION — được gọi qua self-proxy
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyCheckInResponseDto executeCheckInTransaction(UUID accountId) {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 1. Idempotency check — lớp bảo vệ Application (DB constraint là lớp cuối cùng)
        if (checkInRepository.existsByAccountIdAndCheckInDate(accountId, today)) {
            log.warn("Duplicate check-in attempt for account [{}] on [{}]", accountId, today);
            throw new CoinException(CoinErrorCode.ALREADY_CHECKED_IN);
        }

        // 2. Tính streak: hôm qua có điểm danh không?
        int streak = checkInRepository
                .findByAccountIdAndCheckInDate(accountId, yesterday)
                .map(record -> record.getConsecutiveDays() + 1)
                .orElse(1);  // Không có hôm qua → streak mới bắt đầu từ 1

        // 3. Lấy cấu hình kinh tế hiện hành (từ Caffeine Cache hoặc DB nếu cache miss)
        CoinEconomyConfigResponseDto currentConfig = configService.getConfig();

        // 4. Tính toán phần thưởng dựa trên cấu hình động
        BigDecimal reward = calculateReward(streak, currentConfig);

        // 5. Lưu bản ghi điểm danh vào DB
        DailyCheckIn checkInRecord = DailyCheckIn.builder()
                .accountId(accountId)
                .checkInDate(today)
                .consecutiveDays(streak)
                .rewardAmount(reward)
                .build();
        DailyCheckIn savedRecord = checkInRepository.save(checkInRecord);

        // 6. Cộng coin vào ví qua Core Ledger
        // creditCoin tự quản lý Redis lock riêng (lock:coin_transaction:{accountId})
        // → an toàn khi gọi lồng trong transaction này
        String description = String.format("Điểm danh hàng ngày - Chuỗi %d ngày", streak);
        coinWalletService.creditCoin(
                accountId,
                reward,
                CoinReferenceType.DAILY_CHECK_IN,                        // referenceType: phân loại giao dịch
                savedRecord.getCheckinId().toString(),   // referenceId: trỏ đến DailyCheckIn record
                description
        );

        log.info("Check-in success | account=[{}] streak={} reward={}", accountId, streak, reward);

        return DailyCheckInResponseDto.builder()
                .rewardAmount(reward)
                .currentStreak(streak)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tính phần thưởng dựa trên milestone streak và cấu hình động từ Admin.
     *
     * <pre>
     * Bội số 30 (ngày 30, 60, 90...) → config.milestone30Reward
     * Bội số 14 (ngày 14, 28, 42...) → config.milestone14Reward
     * Bội số 7  (ngày 7, 21, 35...)  → config.milestone7Reward
     * Tất cả ngày còn lại            → config.dailyCheckInBase
     * </pre>
     *
     * Lưu ý: Kiểm tra bội số 30 trước 14, 14 trước 7 để milestone lớn hơn được ưu tiên
     * (ví dụ: ngày 42 = bội số 14 VÀ bội số 7 → nhận milestone14Reward, không phải milestone7Reward).
     *
     * @param streak Số ngày liên tiếp hiện tại
     * @param config Cấu hình kinh tế Coin hiện hành (đọc từ Cache)
     * @return Số coin được thưởng cho lần điểm danh này
     */
    private BigDecimal calculateReward(int streak, CoinEconomyConfigResponseDto config) {
        if (streak % 30 == 0) return config.getMilestone30Reward();
        if (streak % 14 == 0) return config.getMilestone14Reward();
        if (streak % 7  == 0) return config.getMilestone7Reward();
        return config.getDailyCheckInBase();
    }
}
