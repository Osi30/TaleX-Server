package com.talex.server.services.creator.impls;

import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.response.PayoutTransactionResponseDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.entities.creator.PayoutTransaction;
import com.talex.server.enums.BankBin;
import com.talex.server.enums.PayoutStatus;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.repositories.transaction.PayoutTransactionRepository;
import com.talex.server.services.creator.PaymentProfileService;
import com.talex.server.services.payout.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorPayoutServiceImpl Tests")
class CreatorPayoutServiceImplTest {

    @Mock
    private CreatorMonthlySettlementRepository settlementRepository;
    @Mock
    private PayoutTransactionRepository payoutTransactionRepository;
    @Mock
    private PaymentProfileService paymentProfileService;
    @Mock
    private PayoutService payoutService;

    @InjectMocks
    private CreatorPayoutServiceImpl service;

    private CreatorMonthlySettlement sampleSettlement;
    private Creator sampleCreator;
    private PaymentProfileResponseDto primaryProfile;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        Account account = Account.builder().accountId(accountId).build();
        sampleCreator = Creator.builder().creatorId("creator-1").account(account).build();

        sampleSettlement = CreatorMonthlySettlement.builder()
                .creatorMonthlySettlementId("settlement-1")
                .settlementMonth("2026-07")
                .netPayoutAmount(BigDecimal.valueOf(1000000))
                .status(SettlementStatus.APPROVED)
                .creator(sampleCreator)
                .build();

        primaryProfile = PaymentProfileResponseDto.builder()
                .bankCode(BankBin.MBBANK)
                .accountNumber("123456789")
                .accountName("JOHN DOE")
                .build();
    }

    // =========================================================================
    // processMonthlyPayout
    // =========================================================================

    @Test
    @DisplayName("processMonthlyPayout - Settlements empty returns empty batch request")
    void processMonthlyPayout_EmptySettlements() {
        when(settlementRepository.findBySettlementMonthAndStatus("2026-07", SettlementStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        BatchPayoutRequestDto res = service.processMonthlyPayout("2026-07", false);

        assertThat(res).isNotNull();
        assertThat(res.getPayouts()).isEmpty();
        verify(payoutService, never()).createBatchPayout(any());
    }

    @Test
    @DisplayName("processMonthlyPayout - Demo mode returns batch request without sending to PayoutService")
    void processMonthlyPayout_DemoMode() {
        when(settlementRepository.findBySettlementMonthAndStatus("2026-07", SettlementStatus.APPROVED))
                .thenReturn(List.of(sampleSettlement));
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);

        BatchPayoutRequestDto res = service.processMonthlyPayout("2026-07", true);

        assertThat(res.getPayouts()).hasSize(1);
        assertThat(res.getPayouts().get(0).getAmount()).isEqualTo(1000000L);
        verify(payoutService, never()).createBatchPayout(any());
    }

    @Test
    @DisplayName("processMonthlyPayout - Real execution calls payoutService and skips invalid settlements")
    void processMonthlyPayout_RealExecution_WithSkippedItems() {
        // Settlement 1: Valid
        // Settlement 2: No creator
        CreatorMonthlySettlement noCreatorSettlement = CreatorMonthlySettlement.builder()
                .creatorMonthlySettlementId("settlement-2")
                .netPayoutAmount(BigDecimal.valueOf(500000))
                .status(SettlementStatus.APPROVED)
                .build();

        // Settlement 3: No primary profile
        UUID acc3 = UUID.randomUUID();
        Creator creator3 = Creator.builder().creatorId("c3").account(Account.builder().accountId(acc3).build()).build();
        CreatorMonthlySettlement noProfileSettlement = CreatorMonthlySettlement.builder()
                .creatorMonthlySettlementId("settlement-3")
                .netPayoutAmount(BigDecimal.valueOf(500000))
                .status(SettlementStatus.APPROVED)
                .creator(creator3)
                .build();

        // Settlement 4: Amount <= 0
        UUID acc4 = UUID.randomUUID();
        Creator creator4 = Creator.builder().creatorId("c4").account(Account.builder().accountId(acc4).build()).build();
        CreatorMonthlySettlement zeroAmountSettlement = CreatorMonthlySettlement.builder()
                .creatorMonthlySettlementId("settlement-4")
                .netPayoutAmount(BigDecimal.ZERO)
                .status(SettlementStatus.APPROVED)
                .creator(creator4)
                .build();

        when(settlementRepository.findBySettlementMonthAndStatus("2026-07", SettlementStatus.APPROVED))
                .thenReturn(List.of(sampleSettlement, noCreatorSettlement, noProfileSettlement, zeroAmountSettlement));

        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);
        when(paymentProfileService.getPrimaryProfile(acc3)).thenReturn(null);
        when(paymentProfileService.getPrimaryProfile(acc4)).thenReturn(primaryProfile);

        BatchPayoutRequestDto res = service.processMonthlyPayout("2026-07", false);

        assertThat(res.getPayouts()).hasSize(1);
        verify(payoutService, times(1)).createBatchPayout(res);
    }

    // =========================================================================
    // processSingleSettlementPayout
    // =========================================================================

    @Test
    @DisplayName("processSingleSettlementPayout - NotFound throws RuntimeException")
    void processSingleSettlement_NotFound() {
        when(settlementRepository.findByCreatorMonthlySettlementId("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processSingleSettlementPayout("invalid", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy bản ghi quyết toán");
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Status not APPROVED throws IllegalStateException")
    void processSingleSettlement_NotApproved() {
        sampleSettlement.setStatus(SettlementStatus.CALCULATED);
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));

        assertThatThrownBy(() -> service.processSingleSettlementPayout("settlement-1", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không ở trạng thái APPROVED");
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Null Creator or primary profile missing or amount <= 0 throws IllegalStateException")
    void processSingleSettlement_ValidationFailures() {
        // Null Creator
        sampleSettlement.setCreator(null);
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));
        assertThatThrownBy(() -> service.processSingleSettlementPayout("settlement-1", false))
                .isInstanceOf(IllegalStateException.class);

        // Profile missing
        sampleSettlement.setCreator(sampleCreator);
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(null);
        assertThatThrownBy(() -> service.processSingleSettlementPayout("settlement-1", false))
                .isInstanceOf(IllegalStateException.class);

        // Amount <= 0
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);
        sampleSettlement.setNetPayoutAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.processSingleSettlementPayout("settlement-1", false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Demo mode")
    void processSingleSettlement_Demo() {
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);

        BatchPayoutRequestDto res = service.processSingleSettlementPayout("settlement-1", true);

        assertThat(res.getPayouts()).hasSize(1);
        verify(payoutService, never()).createBatchPayout(any());
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Gateway success updates PayoutTransaction SUCCESS and Settlement PAID")
    void processSingleSettlement_GatewaySuccess() {
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);

        BatchPayoutDataResponseDto responseDto = new BatchPayoutDataResponseDto();
        responseDto.setId("gateway-batch-1");
        PayoutTransactionResponseDto txnRes = new PayoutTransactionResponseDto();
        txnRes.setId("gateway-txn-1");
        responseDto.setTransactions(List.of(txnRes));

        when(payoutService.createBatchPayout(any())).thenReturn(responseDto);

        service.processSingleSettlementPayout("settlement-1", false);

        verify(payoutTransactionRepository).save(argThat(tx ->
                tx.getStatus() == PayoutStatus.SUCCESS &&
                "gateway-batch-1".equals(tx.getGatewayBatchId())
        ));
        verify(settlementRepository).save(argThat(st ->
                st.getStatus() == SettlementStatus.PAID
        ));
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Gateway returns empty transactions updates PayoutTransaction FAILED")
    void processSingleSettlement_GatewayEmptyResponse() {
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);

        BatchPayoutDataResponseDto responseDto = new BatchPayoutDataResponseDto();
        responseDto.setTransactions(Collections.emptyList());

        when(payoutService.createBatchPayout(any())).thenReturn(responseDto);

        service.processSingleSettlementPayout("settlement-1", false);

        verify(payoutTransactionRepository).save(argThat(tx ->
                tx.getStatus() == PayoutStatus.FAILED &&
                tx.getFailureReason().contains("Cổng thanh toán không trả về giao dịch hợp lệ")
        ));
    }

    @Test
    @DisplayName("processSingleSettlementPayout - Gateway exception updates FAILED and rethrows")
    void processSingleSettlement_GatewayException() {
        when(settlementRepository.findByCreatorMonthlySettlementId("settlement-1")).thenReturn(Optional.of(sampleSettlement));
        when(paymentProfileService.getPrimaryProfile(accountId)).thenReturn(primaryProfile);

        when(payoutService.createBatchPayout(any())).thenThrow(new RuntimeException("Connection timeout"));

        assertThatThrownBy(() -> service.processSingleSettlementPayout("settlement-1", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Thực thi chuyển tiền Payout thất bại");

        verify(payoutTransactionRepository).save(argThat(tx ->
                tx.getStatus() == PayoutStatus.FAILED &&
                "Connection timeout".equals(tx.getFailureReason())
        ));
    }
}
