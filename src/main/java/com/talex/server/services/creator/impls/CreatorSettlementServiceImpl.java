package com.talex.server.services.creator.impls;

import com.talex.server.entities.config.TaxConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.services.config.TaxConfigService;
import com.talex.server.services.creator.CreatorSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorSettlementServiceImpl implements CreatorSettlementService {

    private final CreatorRepository creatorRepository;
    private final CreatorMonthlySettlementRepository settlementRepository;
    private final TaxConfigService taxConfigService;

    private static final BigDecimal MIN_BALANCE_THRESHOLD = BigDecimal.valueOf(2000);
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional
    public List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo) {
        // 1. Tính settlementMonth là tháng hiện tại trừ đi 1 tháng (ví dụ: 2026-07)
        String settlementMonth = LocalDate.now().minusMonths(1).format(MONTH_YEAR_FORMATTER);
        log.info("Bắt đầu xử lý quyết toán Creator cho tháng: {} (isDemo: {})", settlementMonth, isDemo);

        // 2. Query Creator thỏa điều kiện: currentBalance >= 2000, isBanned = false, status = ACTIVE
        List<Creator> eligibleCreators = creatorRepository
                .findByCurrentBalanceGreaterThanEqualAndIsBannedFalseAndAccount_Status(
                        MIN_BALANCE_THRESHOLD,
                        AccountStatus.ACTIVE
                );

        if (eligibleCreators.isEmpty()) {
            log.info("Không có Creator nào đủ điều kiện quyết toán cho tháng {}", settlementMonth);
            return List.of();
        }

        // 3. Lấy thông tin TaxConfig để tính tổng tỷ lệ thuế (VAT + PIT)
        TaxConfig taxConfig = taxConfigService.getTaxConfigEntity();
        double pit = taxConfig.getPit() != null ? taxConfig.getPit() : 0.0;
        long minPitAmount = taxConfig.getMinPitAmount();
        double totalTaxRate = 0.0;

        List<CreatorMonthlySettlement> settlements = new ArrayList<>();

        // 4. Duyệt qua danh sách Creator và tạo bản ghi Settlement
        for (Creator creator : eligibleCreators) {
            BigDecimal grossAmount = creator.getCurrentBalance() != null ? creator.getCurrentBalance() : BigDecimal.ZERO;

            // Lớn hơn hoặc bằng min
            if (grossAmount.compareTo(BigDecimal.valueOf(minPitAmount)) >= 0) {
                totalTaxRate = pit;
            }

            // taxWithheldAmount = grossAmount * taxRate
            BigDecimal taxWithheldAmount = grossAmount.multiply(BigDecimal.valueOf(totalTaxRate));

            // netPayoutAmount = grossAmount - taxWithheldAmount
            BigDecimal netPayoutAmount = grossAmount.subtract(taxWithheldAmount);

            CreatorMonthlySettlement settlement = CreatorMonthlySettlement.builder()
                    .settlementMonth(settlementMonth)
                    .grossAmount(grossAmount)
                    .taxRate(totalTaxRate)
                    .taxWithheldAmount(taxWithheldAmount)
                    .netPayoutAmount(netPayoutAmount)
                    .status(SettlementStatus.CALCULATED)
                    .creator(creator)
                    .build();

            settlements.add(settlement);
            log.info("Quyết toán CreatorId: {}, Gross: {}, TaxRate: {}, TaxWithheld: {}, Net: {}",
                    creator.getCreatorId(), grossAmount, totalTaxRate, taxWithheldAmount, netPayoutAmount);
        }

        // 5. Nếu không phải isDemo thì thực hiện lưu vào CSDL
        if (!isDemo) {
            settlementRepository.saveAll(settlements);
            log.info("Đã lưu thành công {} bản ghi CreatorMonthlySettlement vào CSDL", settlements.size());
        }

        return settlements;
    }
}