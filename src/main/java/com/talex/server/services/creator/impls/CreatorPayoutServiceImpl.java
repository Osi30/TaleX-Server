package com.talex.server.services.creator.impls;

import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.request.PayoutItemRequestDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.transaction.SettlementStatus;
import com.talex.server.repositories.creator.CreatorMonthlySettlementRepository;
import com.talex.server.services.creator.CreatorPayoutService;
import com.talex.server.services.creator.IPaymentProfileService;
import com.talex.server.services.payout.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorPayoutServiceImpl implements CreatorPayoutService {

    private final CreatorMonthlySettlementRepository settlementRepository;
    private final IPaymentProfileService paymentProfileService;
    private final PayoutService payoutService;

    @Override
    @Transactional
    public BatchPayoutRequestDto processMonthlyPayout(String monthYear, boolean isDemo) {
        log.info("Bắt đầu chuẩn bị lô Payout quyết toán cho tháng: {} (isDemo: {})", monthYear, isDemo);

        // 1. Lấy danh sách CreatorMonthlySettlement có status = CALCULATED trong tháng truyền vào
        List<CreatorMonthlySettlement> settlements = settlementRepository
                .findBySettlementMonthAndStatus(monthYear, SettlementStatus.CALCULATED);

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
            Long amountToPay = settlement.getNetPayoutAmount() != null
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
}