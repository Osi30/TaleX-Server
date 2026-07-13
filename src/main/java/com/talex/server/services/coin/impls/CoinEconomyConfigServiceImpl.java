package com.talex.server.services.coin.impls;

import com.talex.server.dtos.requests.coin.CoinEconomyConfigRequestDto;
import com.talex.server.dtos.responses.coin.CoinEconomyConfigResponseDto;
import com.talex.server.entities.coin.CoinEconomyConfig;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.CoinEconomyConfigRepository;
import com.talex.server.services.coin.ICoinEconomyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ cấu hình kinh tế Coin.
 *
 * <h3>Chiến lược Cache (Caffeine Local Cache):</h3>
 * <ul>
 *   <li>{@code getConfig()} — {@code @Cacheable}: Lấy từ cache trước, chỉ query DB khi cache miss.</li>
 *   <li>{@code updateConfig()} — {@code @CacheEvict(allEntries=true)}: Xóa toàn bộ cache ngay khi
 *       Admin lưu config mới, đảm bảo lần gọi {@code getConfig()} kế tiếp luôn lấy bản mới nhất.</li>
 * </ul>
 *
 * <h3>Chiến lược lưu lịch sử:</h3>
 * <p>
 * Mỗi lần Admin cập nhật sẽ INSERT bản ghi MỚI (không UPDATE bản ghi cũ).
 * {@code findFirstByOrderByCreatedAtDesc()} luôn lấy bản mới nhất làm config hiện hành.
 * Bản ghi cũ vẫn còn trong DB → audit trail đầy đủ mọi thay đổi.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoinEconomyConfigServiceImpl implements ICoinEconomyConfigService {

    private final CoinEconomyConfigRepository configRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     * <p>
     * Fallback: Nếu bảng {@code coin_economy_configs} chưa có bản ghi nào (DB mới hoặc bị xóa),
     * tự động khởi tạo và lưu default config thay vì throw exception.
     * Điều này đảm bảo hệ thống LUÔN có config hợp lệ để tính toán phần thưởng.
     * </p>
     */
    @Override
    @Cacheable(value = "COIN_ECONOMY_CONFIG", key = "'CURRENT_CONFIG'",
               cacheManager = "localCacheManager")
    @Transactional
    public CoinEconomyConfigResponseDto getConfig() {
        CoinEconomyConfig config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    log.info("Coin Economy Config is empty. Initializing fallback defaults.");
                    CoinEconomyConfig defaultConfig = CoinEconomyConfig.builder()
                            .dailyCheckInBase(new BigDecimal("10.0000"))
                            .milestone7Reward(new BigDecimal("20.0000"))
                            .milestone14Reward(new BigDecimal("30.0000"))
                            .milestone30Reward(new BigDecimal("50.0000"))
                            .vndPerCoin(new BigDecimal("1.0000"))
                            .build();
                    defaultConfig.markCreatedBy("SYSTEM");
                    return configRepository.save(defaultConfig);
                });

        return CoinEconomyConfigResponseDto.fromEntity(config);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng:
     * <ol>
     *   <li>Validate logic nghiệp vụ (tất cả > 0, và base &lt; m7 &lt; m14 &lt; m30).</li>
     *   <li>INSERT bản ghi config mới với {@code createdBy = adminId}.</li>
     *   <li>{@code @CacheEvict} tự động xóa cache sau khi method return thành công.</li>
     * </ol>
     * </p>
     */
    @Override
    @CacheEvict(value = "COIN_ECONOMY_CONFIG", allEntries = true,
                cacheManager = "localCacheManager")
    @Transactional(rollbackFor = Exception.class)
    public CoinEconomyConfigResponseDto updateConfig(CoinEconomyConfigRequestDto request, UUID adminId) {
        // 1. Validate logic nghiệp vụ — fail-fast trước khi ghi DB
        validateConfigLogic(request);

        // 2. INSERT bản ghi mới → giữ toàn bộ lịch sử thay đổi
        CoinEconomyConfig newConfig = CoinEconomyConfig.builder()
                .dailyCheckInBase(request.getDailyCheckInBase())
                .milestone7Reward(request.getMilestone7Reward())
                .milestone14Reward(request.getMilestone14Reward())
                .milestone30Reward(request.getMilestone30Reward())
                .vndPerCoin(request.getVndPerCoin())
                .build();

        newConfig.markCreatedBy(adminId.toString());

        CoinEconomyConfig savedConfig = configRepository.save(newConfig);
        log.info("Admin [{}] updated Coin Economy Config | base={} m7={} m14={} m30={}",
                adminId,
                request.getDailyCheckInBase(),
                request.getMilestone7Reward(),
                request.getMilestone14Reward(),
                request.getMilestone30Reward());

        return CoinEconomyConfigResponseDto.fromEntity(savedConfig);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra tính hợp lệ về mặt logic nghiệp vụ của cấu hình.
     *
     * <pre>
     * Quy tắc: base > 0, m7 > 0, m14 > 0, m30 > 0
     *          VÀ base < m7 < m14 < m30
     * </pre>
     *
     * @throws CoinException (INVALID_AMOUNT, 400) nếu vi phạm bất kỳ quy tắc nào
     */
    private void validateConfigLogic(CoinEconomyConfigRequestDto request) {
        BigDecimal base = request.getDailyCheckInBase();
        BigDecimal m7   = request.getMilestone7Reward();
        BigDecimal m14  = request.getMilestone14Reward();
        BigDecimal m30  = request.getMilestone30Reward();

        // Guard: tất cả phải > 0 (lớp validation thứ 2 sau @Positive ở DTO)
        if (base.compareTo(BigDecimal.ZERO) <= 0 || m7.compareTo(BigDecimal.ZERO) <= 0
                || m14.compareTo(BigDecimal.ZERO) <= 0 || m30.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoinException(CoinErrorCode.INVALID_AMOUNT,
                    "Tất cả phần thưởng phải lớn hơn 0");
        }

        // Guard: thứ tự tăng dần — milestone lớn hơn phải có phần thưởng lớn hơn
        if (base.compareTo(m7) >= 0 || m7.compareTo(m14) >= 0 || m14.compareTo(m30) >= 0) {
            throw new CoinException(CoinErrorCode.INVALID_AMOUNT,
                    "Lỗi logic cấu hình: Thưởng Cơ bản < Mốc 7 < Mốc 14 < Mốc 30");
        }

        if (request.getVndPerCoin().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoinException(CoinErrorCode.INVALID_AMOUNT,
                    "Tỷ giá VNĐ/Coin phải lớn hơn 0");
        }
    }
}
