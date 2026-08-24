package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.filters.CreatorSettlementFilterRequestDto;
import com.talex.server.dtos.settlement.request.UpdateSettlementStatusRequestDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.config.SettlementConfig;
import com.talex.server.entities.config.TaxConfig;
import com.talex.server.entities.auth.Account;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorSettlementServiceImpl Tests")
class CreatorSettlementServiceImplTest {

    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private RevenueTransactionRepository revenueTransactionRepository;
    @Mock
    private CreatorMonthlySettlementRepository settlementRepository;
    @Mock
    private TaxConfigService taxConfigService;
    @Mock
    private SettlementConfigService settlementConfigService;
    @Mock
    private CreatorSettlementMapper settlementMapper;

    @InjectMocks
    private CreatorSettlementServiceImpl service;

    private Creator activeCreator;
    private Creator bannedCreator;
    private TaxConfig taxConfig;
    private SettlementConfig settlementConfig;

    @BeforeEach
    void setUp() {
        Account activeAccount = Account.builder().accountId(UUID.randomUUID()).status(AccountStatus.ACTIVE).build();
        activeCreator = Creator.builder()
                .creatorId("creator-active")
                .account(activeAccount)
                .isBanned(false)
                .currentBalance(BigDecimal.valueOf(100000))
                .build();

        Account bannedAccount = Account.builder().accountId(UUID.randomUUID()).status(AccountStatus.BANNED).build();
        bannedCreator = Creator.builder()
                .creatorId("creator-banned")
                .account(bannedAccount)
                .isBanned(true)
                .currentBalance(BigDecimal.valueOf(50000))
                .build();

        taxConfig = TaxConfig.builder().pit(0.1).minPitAmount(2000000L).build();
        settlementConfig = SettlementConfig.builder().minBalanceThreshold(BigDecimal.valueOf(2000)).build();
    }

    @Test
    @DisplayName("processMonthlySettlement - Default overload minus 1 month")
    void processMonthlySettlement_DefaultOverload() {
        when(revenueTransactionRepository.findUnsettledTransactionsUpToMonth(any()))
                .thenReturn(Collections.emptyList());

        List<CreatorMonthlySettlement> res = service.processMonthlySettlement(true);
        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("processMonthlySettlement - Empty unsettled transactions")
    void processMonthlySettlement_EmptyTransactions() {
        when(revenueTransactionRepository.findUnsettledTransactionsUpToMonth(any()))
                .thenReturn(Collections.emptyList());

        List<CreatorMonthlySettlement> res = service.processMonthlySettlement(false, "2026-07");
        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("processMonthlySettlement - Active creator (above threshold, with PIT) & Banned creator & Low balance creator")
    void processMonthlySettlement_ComplexScenarios() {
        when(taxConfigService.getTaxConfigEntity()).thenReturn(taxConfig);
        when(settlementConfigService.getSettlementConfigEntity()).thenReturn(settlementConfig);

        // Active creator: 3,000,000 revenue - 500,000 penalty = 2,500,000 gross (>= 2,000,000 PIT min -> 10% tax)
        RevenueTransaction tx1 = RevenueTransaction.builder().creator(activeCreator).amount(BigDecimal.valueOf(3000000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();
        RevenueTransaction tx2 = RevenueTransaction.builder().creator(activeCreator).amount(BigDecimal.valueOf(500000)).revenueTransactionType(RevenueTransactionType.PENALTY_DEDUCTION).build();

        // Banned creator: 100,000 gross -> status FROZEN_PENALTY
        RevenueTransaction tx3 = RevenueTransaction.builder().creator(bannedCreator).amount(BigDecimal.valueOf(100000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();

        // Low balance creator: 1,000 gross (< 2,000 minBalanceThreshold) -> deferred / skipped
        Creator lowBalanceCreator = Creator.builder().creatorId("low").account(Account.builder().status(AccountStatus.ACTIVE).build()).isBanned(false).build();
        RevenueTransaction tx4 = RevenueTransaction.builder().creator(lowBalanceCreator).amount(BigDecimal.valueOf(1000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();

        when(revenueTransactionRepository.findUnsettledTransactionsUpToMonth(any()))
                .thenReturn(List.of(tx1, tx2, tx3, tx4));

        when(settlementRepository.save(any(CreatorMonthlySettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        List<CreatorMonthlySettlement> res = service.processMonthlySettlement(false, "2026-07");

        // Should return 2 settlements (activeCreator and bannedCreator, lowBalanceCreator skipped)
        assertThat(res).hasSize(2);

        CreatorMonthlySettlement activeSettlement = res.stream().filter(s -> s.getCreator().equals(activeCreator)).findFirst().orElseThrow();
        assertThat(activeSettlement.getStatus()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(activeSettlement.getGrossAmount()).isEqualTo(BigDecimal.valueOf(2500000));
        assertThat(activeSettlement.getTaxWithheldAmount()).isEqualTo(BigDecimal.valueOf(250000.0));
        assertThat(activeSettlement.getNetPayoutAmount()).isEqualTo(BigDecimal.valueOf(2250000.0));

        CreatorMonthlySettlement bannedSettlement = res.stream().filter(s -> s.getCreator().equals(bannedCreator)).findFirst().orElseThrow();
        assertThat(bannedSettlement.getStatus()).isEqualTo(SettlementStatus.FROZEN_PENALTY);

        verify(revenueTransactionRepository, times(2)).saveAll(anyList());
        verify(creatorRepository).save(activeCreator);
    }

    @Test
    @DisplayName("processMonthlySettlement - Demo mode does not persist DB changes")
    void processMonthlySettlement_DemoMode() {
        when(taxConfigService.getTaxConfigEntity()).thenReturn(taxConfig);
        when(settlementConfigService.getSettlementConfigEntity()).thenReturn(settlementConfig);

        RevenueTransaction tx1 = RevenueTransaction.builder().creator(activeCreator).amount(BigDecimal.valueOf(100000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();
        when(revenueTransactionRepository.findUnsettledTransactionsUpToMonth(any())).thenReturn(List.of(tx1));

        List<CreatorMonthlySettlement> res = service.processMonthlySettlement(true, "2026-07");

        assertThat(res).hasSize(1);
        verify(settlementRepository, never()).save(any());
        verify(creatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSettlementStatus - Validation and status transitions")
    void updateSettlementStatus() {
        CreatorMonthlySettlement settlement = CreatorMonthlySettlement.builder()
                .creatorMonthlySettlementId("s-1")
                .status(SettlementStatus.CALCULATED)
                .build();

        when(settlementRepository.findByCreatorMonthlySettlementId("s-1")).thenReturn(Optional.of(settlement));

        // Missing note for UNDER_REVIEW or FORFEITED
        UpdateSettlementStatusRequestDto reqNoNote = UpdateSettlementStatusRequestDto.builder().status(SettlementStatus.UNDER_REVIEW).build();
        assertThatThrownBy(() -> service.updateSettlementStatus("s-1", reqNoNote))
                .isInstanceOf(IllegalArgumentException.class);

        // Invalid transition from PAID
        settlement.setStatus(SettlementStatus.PAID);
        UpdateSettlementStatusRequestDto reqValidNote = UpdateSettlementStatusRequestDto.builder().status(SettlementStatus.UNDER_REVIEW).note("Need check").build();
        assertThatThrownBy(() -> service.updateSettlementStatus("s-1", reqValidNote))
                .isInstanceOf(IllegalStateException.class);

        // Valid transition from CALCULATED to APPROVED
        settlement.setStatus(SettlementStatus.CALCULATED);
        UpdateSettlementStatusRequestDto reqApproved = UpdateSettlementStatusRequestDto.builder().status(SettlementStatus.APPROVED).build();
        when(settlementRepository.save(settlement)).thenReturn(settlement);
        CreatorSettlementDetailResponseDto dto = CreatorSettlementDetailResponseDto.builder().creatorMonthlySettlementId("s-1").build();
        when(settlementMapper.toDetailResponseDto(settlement)).thenReturn(dto);

        CreatorSettlementDetailResponseDto res = service.updateSettlementStatus("s-1", reqApproved);
        assertThat(res).isEqualTo(dto);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.APPROVED);
    }

    @Test
    @DisplayName("filterSettlements & getSettlementById")
    void filterAndGetById() {
        // getSettlementById
        CreatorMonthlySettlement settlement = CreatorMonthlySettlement.builder().creatorMonthlySettlementId("s-1").build();
        when(settlementRepository.findByCreatorMonthlySettlementId("s-1")).thenReturn(Optional.of(settlement));
        when(settlementMapper.toDetailResponseDto(settlement)).thenReturn(CreatorSettlementDetailResponseDto.builder().creatorMonthlySettlementId("s-1").build());
        assertThat(service.getSettlementById("s-1")).isNotNull();

        when(settlementRepository.findByCreatorMonthlySettlementId("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSettlementById("invalid")).isInstanceOf(RuntimeException.class);

        // filterSettlements
        CreatorSettlementFilterRequestDto filterReq = CreatorSettlementFilterRequestDto.builder()
                .page(1)
                .pageSize(10)
                .statuses(new String[]{"APPROVED"})
                .sortBy("grossAmount")
                .sortDirection("ASC")
                .build();

        Page<CreatorMonthlySettlement> page = new PageImpl<>(List.of(settlement));
        when(settlementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(settlementMapper.toResponseDto(settlement)).thenReturn(CreatorSettlementResponseDto.builder().creatorMonthlySettlementId("s-1").build());

        BasePageResponse<CreatorSettlementResponseDto> res = service.filterSettlements(filterReq);
        assertThat(res.getContent()).hasSize(1);
    }
}
