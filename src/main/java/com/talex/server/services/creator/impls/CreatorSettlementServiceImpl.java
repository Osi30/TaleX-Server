package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.filters.CreatorSettlementFilterRequestDto;
import com.talex.server.dtos.settlement.request.UpdateSettlementStatusRequestDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.config.SettlementConfig;
import com.talex.server.entities.config.TaxConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.mappers.settlement.CreatorSettlementMapper;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.config.SettlementConfigService;
import com.talex.server.services.config.TaxConfigService;
import com.talex.server.services.creator.CreatorSettlementService;
import com.talex.server.specifications.CreatorMonthlySettlementSpec;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorSettlementServiceImpl implements CreatorSettlementService {

    private final CreatorRepository creatorRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final CreatorMonthlySettlementRepository settlementRepository;
    private final TaxConfigService taxConfigService;
    private final SettlementConfigService settlementConfigService;
    private final CreatorSettlementMapper settlementMapper;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional
    public List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo) {
        // Mặc định quét cho tháng vừa qua (tháng hiện tại - 1)
        String targetMonthStr = LocalDate.now().minusMonths(1).format(MONTH_YEAR_FORMATTER);
        return processMonthlySettlement(isDemo, targetMonthStr);
    }

    @Override
    @Transactional
    public List<CreatorMonthlySettlement> processMonthlySettlement(boolean isDemo, String targetMonthStr) {
        log.info("=== BẮT ĐẦU XỬ LÝ QUYẾT TOÁN CHO THÁNG: {} (isDemo: {}) ===", targetMonthStr, isDemo);

        // 1. Xác định mốc thời gian chốt sổ (Cutoff Date)
        YearMonth yearMonth = YearMonth.parse(targetMonthStr, MONTH_YEAR_FORMATTER);
        LocalDateTime cutoffDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 2. Query tất cả RevenueTransaction chưa đối soát từ targetMonthYear trở về trước
        List<RevenueTransaction> unSettledTransactions =
                revenueTransactionRepository.findUnsettledTransactionsUpToMonth(cutoffDate.toLocalDate());

        if (unSettledTransactions.isEmpty()) {
            log.info("Không tìm thấy giao dịch doanh thu nào chưa đối soát cho tháng {}", targetMonthStr);
            return List.of();
        }

        // 3. Nhóm danh sách transaction theo từng Creator
        Map<Creator, List<RevenueTransaction>> transactionsByCreator = unSettledTransactions.stream()
                .collect(Collectors.groupingBy(RevenueTransaction::getCreator));

        // 4. Lấy cấu hình thuế
        TaxConfig taxConfig = taxConfigService.getTaxConfigEntity();
        double pitRate = taxConfig.getPit() != null ? taxConfig.getPit() : 0.0;
        long minPitAmount = taxConfig.getMinPitAmount();

        // 4.1 Lấy ngưỡng tối thiểu hạn mức
        SettlementConfig settlementConfig = settlementConfigService.getSettlementConfigEntity();
        BigDecimal minBalanceThreshold = settlementConfig.getMinBalanceThreshold() != null
                ? settlementConfig.getMinBalanceThreshold()
                : BigDecimal.ZERO;

        List<CreatorMonthlySettlement> settlements = new ArrayList<>();

        // 5. Duyệt từng Creator để tính toán sổ sách
        for (Map.Entry<Creator, List<RevenueTransaction>> entry : transactionsByCreator.entrySet()) {
            Creator creator = entry.getKey();
            List<RevenueTransaction> creatorTxList = entry.getValue();

            // 5.1 Bóc tách tổng phạt (Penalty) và tổng tiền thu nhập dương
            BigDecimal totalPenaltyAmount = BigDecimal.ZERO;
            BigDecimal grossRevenue = BigDecimal.ZERO;

            for (RevenueTransaction tx : creatorTxList) {
                BigDecimal txAmount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;

                if (tx.getRevenueTransactionType() == RevenueTransactionType.PENALTY_DEDUCTION || txAmount.compareTo(BigDecimal.ZERO) < 0) {
                    totalPenaltyAmount = totalPenaltyAmount.add(txAmount.abs());
                } else {
                    grossRevenue = grossRevenue.add(txAmount);
                }
            }
            BigDecimal grossAmount = grossRevenue.subtract(totalPenaltyAmount);

            // Check account freeze/lock status:
            // 1. Creator is banned
            // 2. Or Account is null / Status is not ACTIVE
            boolean isAccountNotActive = creator.getAccount() == null || creator.getAccount().getStatus() != AccountStatus.ACTIVE;
            boolean isFrozen = Boolean.TRUE.equals(creator.getIsBanned()) || isAccountNotActive;

            // If not freeze and grossAmount < min threshold (2.000 VNĐ)
            // -> Carry the balance over to next month.
            if (!isFrozen && grossAmount.compareTo(minBalanceThreshold) < 0) {
                log.info("Creator [{}] có số dư chốt ({}) < ngưỡng tối thiểu ({}). Tự động dồn sổ sang kỳ sau.",
                        creator.getCreatorId(), grossAmount, minBalanceThreshold);
                continue;
            }

            // 5.2 Calculate PIT
            double appliedTaxRate = 0.0;
            if (grossAmount.compareTo(BigDecimal.valueOf(minPitAmount)) >= 0) {
                appliedTaxRate = pitRate;
            }
            BigDecimal taxWithheldAmount = grossAmount.multiply(BigDecimal.valueOf(appliedTaxRate))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal netPayoutAmount = grossAmount.subtract(taxWithheldAmount);

            // 5.3 Xác định trạng thái Settlement
            SettlementStatus status = isFrozen ? SettlementStatus.FROZEN_PENALTY : SettlementStatus.CALCULATED;

            // 5.4 Khởi tạo Entity Settlement
            CreatorMonthlySettlement settlement = CreatorMonthlySettlement.builder()
                    .settlementMonth(targetMonthStr)
                    .cutoffDate(cutoffDate)
                    .grossAmount(grossAmount)
                    .totalPenaltyAmount(totalPenaltyAmount)
                    .taxRate(appliedTaxRate)
                    .taxWithheldAmount(taxWithheldAmount)
                    .netPayoutAmount(netPayoutAmount)
                    .status(status)
                    .creator(creator)
                    .revenueTransactions(new ArrayList<>(creatorTxList))
                    .build();

            settlements.add(settlement);

            log.info("-> Creator: {}, Status: {}, Gross: {}, Penalty: {}, NetPayout: {}, Số GD gom: {}",
                    creator.getCreatorId(), status, grossAmount, totalPenaltyAmount, netPayoutAmount, creatorTxList.size());
        }

        // 6. Nếu KHÔNG PHẢI DEMO, tiến hành ghi DB & cập nhật liên kết
        if (!isDemo && !settlements.isEmpty()) {
            for (CreatorMonthlySettlement settlement : settlements) {
                List<RevenueTransaction> txs = settlement.getRevenueTransactions();

                // Lưu bản ghi settlement
                CreatorMonthlySettlement savedSettlement = settlementRepository.save(settlement);

                // Cập nhật FK settlement_id vào từng RevenueTransaction
                for (RevenueTransaction tx : txs) {
                    tx.setCreatorMonthlySettlement(savedSettlement);
                }
                revenueTransactionRepository.saveAll(txs);

                // Trừ bớt currentBalance của Creator số tiền đã chốt sổ
                Creator creator = settlement.getCreator();
                BigDecimal updatedBalance = creator.getCurrentBalance().subtract(settlement.getGrossAmount());
                creator.setCurrentBalance(updatedBalance.max(BigDecimal.ZERO));
                creatorRepository.save(creator);
            }
            log.info("Đã lưu thành công {} bản ghi quyết toán và cập nhật sổ cái DB.", settlements.size());
        } else {
            log.info("[DEMO MODE] Đã tính toán xong {} bản ghi quyết toán. KHÔNG LƯU DATABASE.", settlements.size());
        }

        return settlements;
    }

    @Override
    @Transactional
    public CreatorSettlementDetailResponseDto updateSettlementStatus(
            String settlementId,
            UpdateSettlementStatusRequestDto request
    ) {
        CreatorMonthlySettlement settlement = settlementRepository.findByCreatorMonthlySettlementId(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement không tồn tại với ID: " + settlementId));

        SettlementStatus currentStatus = settlement.getStatus();
        SettlementStatus newStatus = request.getStatus();

        // 1. Kiểm tra yêu cầu Lý do (Note) đối với các trạng thái nhạy cảm
        if ((newStatus == SettlementStatus.UNDER_REVIEW || newStatus == SettlementStatus.FORFEITED)
                && ValidationUtils.isNullOrEmpty(request.getNote())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do (note) khi chuyển trạng thái sang " + newStatus);
        }

        // 2. Validate Luồng chuyển đổi trạng thái hợp lệ
        validateStatusTransition(currentStatus, newStatus);

        // 3. Cập nhật thông tin
        settlement.setStatus(newStatus);
        if (!ValidationUtils.isNullOrEmpty(request.getNote())) {
            settlement.setNote(request.getNote());
        }

        CreatorMonthlySettlement updated = settlementRepository.save(settlement);
        log.info("Admin cập nhật Settlement [{}] từ {} -> {}", settlementId, currentStatus, newStatus);

        return settlementMapper.toDetailResponseDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CreatorSettlementResponseDto> filterSettlements(
            CreatorSettlementFilterRequestDto filterRequest
    ) {
        // 1. Build cấu hình sắp xếp (Sort)
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), sort);

        // 2. Thực thi Query với JPA Specification
        Page<CreatorMonthlySettlement> pageResult = settlementRepository.findAll(
                CreatorMonthlySettlementSpec.filterByCriteria(
                        filterRequest.getCriteria(),
                        Arrays.stream(filterRequest.getStatuses()).map(
                                SettlementStatus::valueOf
                        ).toArray(SettlementStatus[]::new)
                ),
                pageable
        );

        // 3. Map kết quả sang Response DTO
        List<CreatorSettlementResponseDto> content = pageResult.stream()
                .map(settlementMapper::toResponseDto)
                .toList();

        return BasePageResponse.<CreatorSettlementResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorSettlementDetailResponseDto getSettlementById(String id) {
        CreatorMonthlySettlement settlement = settlementRepository.findByCreatorMonthlySettlementId(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found with ID: " + id));
        return settlementMapper.toDetailResponseDto(settlement);
    }

    private Sort buildSort(BaseFilterRequestDto filterRequest) {
        String sortDirection = Optional.ofNullable(filterRequest.getSortDirection()).orElse("DESC");
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, normalizeSortProperty(filterRequest.getSortBy()));
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }
        // Cho phép sắp xếp theo grossAmount, netPayoutAmount, status và một số trường thông dụng
        return switch (sortBy) {
            case "grossAmount", "netPayoutAmount", "status", "settlementMonth", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private void validateStatusTransition(SettlementStatus current, SettlementStatus target) {
        if (current == target) {
            return;
        }

        // Không cho phép thay đổi khi đã ở trạng thái kết thúc
        if (current == SettlementStatus.PAID || current == SettlementStatus.FORFEITED) {
            throw new IllegalStateException("Bản ghi quyết toán đã ở trạng thái " + current + ", không thể thay đổi nữa.");
        }

        boolean isValid = switch (current) {
            case CALCULATED -> EnumSet.of(
                    SettlementStatus.APPROVED,
                    SettlementStatus.UNDER_REVIEW,
                    SettlementStatus.FORFEITED,
                    SettlementStatus.FROZEN_PENALTY
            ).contains(target);

            case FROZEN_PENALTY -> EnumSet.of(
                    SettlementStatus.APPROVED,
                    SettlementStatus.UNDER_REVIEW,
                    SettlementStatus.FORFEITED
            ).contains(target);

            case APPROVED -> EnumSet.of(
                    SettlementStatus.PAID,
                    SettlementStatus.UNDER_REVIEW
            ).contains(target);

            case UNDER_REVIEW -> EnumSet.of(
                    SettlementStatus.APPROVED,
                    SettlementStatus.FORFEITED,
                    SettlementStatus.FROZEN_PENALTY
            ).contains(target);

            default -> false;
        };

        if (!isValid) {
            throw new IllegalStateException(
                    String.format("Không thể chuyển trạng thái quyết toán từ %s sang %s", current, target)
            );
        }
    }
}