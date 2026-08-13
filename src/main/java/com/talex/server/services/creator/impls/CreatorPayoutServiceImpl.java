package com.talex.server.services.creator.impls;

import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.request.PayoutItemRequestDto;
import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.response.PayoutTransactionResponseDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.entities.creator.PayoutTransaction;
import com.talex.server.enums.PayoutStatus;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.repositories.transaction.PayoutTransactionRepository;
import com.talex.server.services.creator.CreatorPayoutService;
import com.talex.server.services.creator.PaymentProfileService;
import com.talex.server.services.payout.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorPayoutServiceImpl implements CreatorPayoutService {

    private final CreatorMonthlySettlementRepository settlementRepository;
    private final PayoutTransactionRepository payoutTransactionRepository;
    private final PaymentProfileService paymentProfileService;
    private final PayoutService payoutService;

    @Override
    @Transactional
    public BatchPayoutRequestDto processMonthlyPayout(String monthYear, boolean isDemo) {
        log.info("Bắt đầu chuẩn bị lô Payout quyết toán cho tháng: {} (isDemo: {})", monthYear, isDemo);

        // 1. Lấy danh sách CreatorMonthlySettlement có status = APPROVED trong tháng truyền vào
        List<CreatorMonthlySettlement> settlements = settlementRepository
                .findBySettlementMonthAndStatus(monthYear, SettlementStatus.APPROVED);

        if (settlements.isEmpty()) {
            log.info("Không tìm thấy bản ghi CreatorMonthlySettlement nào ở trạng thái CALCULATED trong tháng {}", monthYear);
            return BatchPayoutRequestDto.builder()
                    .referenceId("BATCH_SETTLEMENT_" + monthYear + "_" + System.currentTimeMillis())
                    .payouts(List.of())
                    .build();
        }

        List<PayoutItemRequestDto> payoutItems = new ArrayList<>();

        // 2. Duyệt qua từng settlement để lấy tài khoản ngân hàng chính (Primary Payment Profile)
        for (CreatorMonthlySettlement settlement : settlements) {
            Creator creator = settlement.getCreator();
            if (creator == null || creator.getAccount() == null) {
                log.warn("Bỏ qua settlementId: {} do không tìm thấy thông tin Creator/Account", settlement.getCreatorMonthlySettlementId());
                continue;
            }

            UUID accountId = creator.getAccount().getAccountId();

            // Gọi getPrimaryProfile từ IPaymentProfileService
            PaymentProfileResponseDto primaryProfile = paymentProfileService.getPrimaryProfile(accountId);

            // Nếu không tìm thấy profile ngân hàng chính thì bỏ qua
            if (primaryProfile == null) {
                log.warn("Bỏ qua CreatorId: {} (AccountId: {}) do chưa thiết lập PaymentProfile chính",
                        creator.getCreatorId(), accountId);
                continue;
            }

            // Chuyển đổi netPayoutAmount (BigDecimal) sang Long
            long amountToPay = settlement.getNetPayoutAmount() != null
                    ? settlement.getNetPayoutAmount().longValue()
                    : 0L;

            if (amountToPay <= 0) {
                log.warn("Bỏ qua SettlementId: {} do số tiền thanh toán netPayoutAmount <= 0", settlement.getCreatorMonthlySettlementId());
                continue;
            }

            // Tạo từng lệnh chi lẻ (PayoutItemRequestDto)
            PayoutItemRequestDto item = PayoutItemRequestDto.builder()
                    .referenceId(settlement.getCreatorMonthlySettlementId())
                    .amount(amountToPay)
                    .description("Gui tien chia - " + monthYear)
                    .toBin(primaryProfile.getBankCode().getBin()) // Lấy mã BIN từ enum BankBin
                    .toAccountNumber(primaryProfile.getAccountNumber())
                    .build();

            payoutItems.add(item);
        }

        // 3. Tổng hợp thành BatchPayoutRequestDto
        BatchPayoutRequestDto batchPayoutRequest = BatchPayoutRequestDto.builder()
                .referenceId("123456")
                .validateDestination(true)
                .payouts(payoutItems)
                .build();

        log.info("Đã tạo thành công BatchPayoutRequestDto cho {} lệnh chi trong tháng {}", payoutItems.size(), monthYear);

        // 4. Nếu là Demo (isDemo = true), dừng lại và trả về request object để kiểm tra
        if (isDemo) {
            log.info("Chế độ Demo kích hoạt. Trả về cấu trúc request mà KHÔNG gửi qua PayOS.");
            return batchPayoutRequest;
        }

        // 5. Nếu không phải Demo, gửi batch payout thật qua PayoutService
        if (!payoutItems.isEmpty()) {
            payoutService.createBatchPayout(batchPayoutRequest);
            log.info("Đã gửi lô lệnh chi thành công tới PayoutService");
        }

        return batchPayoutRequest;
    }

    @Override
    @Transactional
    public BatchPayoutRequestDto processSingleSettlementPayout(String settlementId, boolean isDemo) {
        // 1. Lấy bản ghi Settlement theo ID
        CreatorMonthlySettlement settlement = settlementRepository.findByCreatorMonthlySettlementId(settlementId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi quyết toán với ID: " + settlementId));

        // 2. Validate trạng thái APPROVED
        if (settlement.getStatus() != SettlementStatus.APPROVED) {
            throw new IllegalStateException(
                    String.format("Bản ghi quyết toán %s không ở trạng thái APPROVED (Trạng thái hiện tại: %s)",
                            settlementId, settlement.getStatus())
            );
        }

        // 3. Lấy Creator và Account liên kết
        Creator creator = settlement.getCreator();
        if (creator == null || creator.getAccount() == null) {
            throw new IllegalStateException("Không tìm thấy thông tin Creator/Account liên kết với Settlement ID: " + settlementId);
        }

        UUID accountId = creator.getAccount().getAccountId();

        // 4. Lấy thông tin Ngân hàng chính (Primary Payment Profile)
        PaymentProfileResponseDto primaryProfile = paymentProfileService.getPrimaryProfile(accountId);
        if (primaryProfile == null) {
            throw new IllegalStateException("CreatorId: " + creator.getCreatorId() + " chưa thiết lập PaymentProfile chính");
        }

        // 5. Kiểm tra số tiền Net Payout
        long amountToPay = settlement.getNetPayoutAmount() != null
                ? settlement.getNetPayoutAmount().longValue()
                : 0L;

        if (amountToPay <= 0) {
            throw new IllegalStateException("Số tiền netPayoutAmount <= 0 đối với Settlement ID: " + settlementId);
        }

        // 6. Tạo lệnh chi lẻ PayoutItemRequestDto
        String batchRefId = "SINGLE_PAYOUT_" + settlementId + "_" + System.currentTimeMillis();
        PayoutItemRequestDto item = PayoutItemRequestDto.builder()
                .referenceId(settlement.getCreatorMonthlySettlementId())
                .amount(amountToPay)
                .description("Settlement - " + settlement.getSettlementMonth())
                .toBin(primaryProfile.getBankCode().getBin())
                .toAccountNumber(primaryProfile.getAccountNumber())
                .build();

        // 7. Đóng gói thành BatchPayoutRequestDto với 1 item
        BatchPayoutRequestDto batchPayoutRequest = BatchPayoutRequestDto.builder()
                .referenceId(batchRefId)
                .validateDestination(true)
                .payouts(List.of(item))
                .build();

        // 8. Nếu là Demo (isDemo = true), dừng lại và trả về Object DTO để kiểm tra
        if (isDemo) {
            log.info("[DEMO MODE] Trả về cấu trúc BatchPayoutRequestDto đơn lẻ mà KHÔNG gửi qua PayOS.");
            return batchPayoutRequest;
        }

        // 9. Nếu không phải Demo (isDemo = false), gọi PayoutService thực hiện gửi lệnh chi thật
        PayoutTransaction payoutTxn = PayoutTransaction.builder()
                .batchReferenceId(batchRefId)
                .transactionReferenceId(settlement.getCreatorMonthlySettlementId())
                .amount(BigDecimal.valueOf(amountToPay))
                .status(PayoutStatus.PENDING)
                .toBin(primaryProfile.getBankCode())
                .toAccountNumber(primaryProfile.getAccountNumber())
                .toAccountName(primaryProfile.getAccountName())
                .creatorMonthlySettlement(settlement)
                .build();

        // 9. Thực thi gọi PayoutService trong khối try-catch
        try {
            BatchPayoutDataResponseDto response = payoutService.createBatchPayout(batchPayoutRequest);

            // Kiểm tra response chứa danh sách transactions thành công
            if (response != null && response.getTransactions() != null && !response.getTransactions().isEmpty()) {
                PayoutTransactionResponseDto gatewayTxn = response.getTransactions().getFirst();

                // Cập nhật PayoutTransaction -> SUCCESS
                payoutTxn.setStatus(PayoutStatus.SUCCESS);
                payoutTxn.setGatewayBatchId(response.getId());
                payoutTxn.setPayoutReference(gatewayTxn.getId());
                payoutTxn.setPaidAt(LocalDateTime.now());
                payoutTransactionRepository.save(payoutTxn);

                // Cập nhật CreatorMonthlySettlement -> PAID
                settlement.setStatus(SettlementStatus.PAID);
                settlementRepository.save(settlement);

            } else {
                // Gateway trả về response không thành công hoặc rỗng
                payoutTxn.setStatus(PayoutStatus.FAILED);
                payoutTxn.setFailureReason("Cổng thanh toán không trả về giao dịch hợp lệ");
                payoutTransactionRepository.save(payoutTxn);
            }
        } catch (Exception e) {
            payoutTxn.setStatus(PayoutStatus.FAILED);
            payoutTxn.setFailureReason(e.getMessage());
            payoutTransactionRepository.save(payoutTxn);
            throw new RuntimeException("Thực thi chuyển tiền Payout thất bại: " + e.getMessage(), e);
        }

        return batchPayoutRequest;
    }
}