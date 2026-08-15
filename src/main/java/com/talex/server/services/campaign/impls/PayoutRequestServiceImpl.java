package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.payout.request.BatchPayoutRequestDto;
import com.talex.server.dtos.payout.request.PayoutItemRequestDto;
import com.talex.server.dtos.payout.request.PayoutRequestProcessDto;
import com.talex.server.dtos.payout.response.BatchPayoutDataResponseDto;
import com.talex.server.dtos.payout.response.PayoutRequestResponseDto;
import com.talex.server.dtos.payout.response.PayoutTransactionResponseDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.campaign.CampaignWallet;
import com.talex.server.entities.campaign.CampaignWalletTransaction;
import com.talex.server.entities.campaign.PayoutRequest;
import com.talex.server.entities.campaign.WalletPayoutTransaction;
import com.talex.server.enums.PayoutStatus;
import com.talex.server.enums.engagement.PayoutRequestStatus;
import com.talex.server.enums.engagement.WalletReferenceType;
import com.talex.server.enums.engagement.WalletTransactionType;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.repositories.campaign.CampaignWalletRepository;
import com.talex.server.repositories.campaign.CampaignWalletTransactionRepository;
import com.talex.server.repositories.campaign.PayoutRequestRepository;
import com.talex.server.repositories.campaign.WalletPayoutTransactionRepository;
import com.talex.server.services.campaign.PayoutRequestService;
import com.talex.server.services.creator.PaymentProfileService;
import com.talex.server.services.payout.PayoutService;
import com.talex.server.specifications.campaign.PayoutRequestSpec;
import com.talex.server.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutRequestServiceImpl implements PayoutRequestService {

    private final PayoutRequestRepository payoutRequestRepository;
    private final CampaignWalletRepository campaignWalletRepository;
    private final CampaignWalletTransactionRepository campaignWalletTransactionRepository;
    private final WalletPayoutTransactionRepository walletPayoutTransactionRepository;
    private final PaymentProfileService paymentProfileService;
    private final PayoutService payoutService;

    private static final BigDecimal MIN_PAYOUT_AMOUNT = new BigDecimal("2000");

    @Override
    @Transactional
    public PayoutRequestResponseDto createPayoutRequest(UUID accountId) {
        // 1. Kiểm tra tài khoản nhận tiền chính (Primary Payment Profile)
        PaymentProfileResponseDto primaryProfile = paymentProfileService.getPrimaryProfile(accountId);
        if (primaryProfile == null) {
            throw new PaymentException(
                    PaymentErrorCode.ORDER_NOT_FOUND,
                    "Vui lòng đăng ký tài khoản nhận tiền (Payment Profile chính) trước khi yêu cầu rút tiền."
            );
        }

        // 2. Lấy Ví Campaign và khóa số dư để chống race condition
        CampaignWallet wallet = campaignWalletRepository.findWithLockByAccountId(accountId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.ORDER_NOT_FOUND,
                        "Ví Campaign Wallet không tồn tại"
                ));

        BigDecimal currentBalance = wallet.getBalance();

        // 3. Kiểm tra số dư khả dụng >= 2.000 VNĐ
        if (currentBalance.compareTo(MIN_PAYOUT_AMOUNT) < 0) {
            throw new PaymentException(
                    PaymentErrorCode.INSUFFICIENT_BALANCE,
                    "Số dư ví phải có từ 2,000 VNĐ trở lên mới có thể tạo yêu cầu rút tiền."
            );
        }

        // 4. Trừ số dư về 0
        wallet.setBalance(BigDecimal.ZERO);
        campaignWalletRepository.save(wallet);

        // 5. Khởi tạo Entity PayoutRequest (PENDING)
        PayoutRequest payoutRequest = PayoutRequest.builder()
                .accountId(accountId)
                .amount(currentBalance)
                .status(PayoutRequestStatus.PENDING)
                .paymentProfileId(primaryProfile.getPaymentProfileId())
                .bankName(primaryProfile.getBankCode())
                .bankAccountNumber(primaryProfile.getAccountNumber())
                .bankAccountName(primaryProfile.getAccountName())
                .build();
        payoutRequestRepository.save(payoutRequest);

        // 6. Ghi vết giao dịch CampaignWalletTransaction
        CampaignWalletTransaction walletTx = CampaignWalletTransaction.builder()
                .campaignWallet(wallet)
                .amount(currentBalance)
                .balanceBefore(currentBalance)
                .balanceAfter(BigDecimal.ZERO)
                .transactionType(WalletTransactionType.PAYOUT_REQUEST)
                .referenceType(WalletReferenceType.PAYOUT_REQUEST)
                .referenceId(payoutRequest.getPayoutRequestId())
                .description("Yêu cầu rút tiền về tài khoản ngân hàng " + primaryProfile.getBankCode().getShortName())
                .build();
        campaignWalletTransactionRepository.save(walletTx);

        return toResponseDto(payoutRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<PayoutRequestResponseDto> getPayoutRequests(Map<String, Object> criteria, Integer page, Integer pageSize) {
        Pageable pageable = PageUtils.buildPageable(page, pageSize);
        Page<PayoutRequestResponseDto> pageResult = payoutRequestRepository
                .findAll(PayoutRequestSpec.filterByCriteria(criteria), pageable)
                .map(this::toResponseDto);

        return BasePageResponse.<PayoutRequestResponseDto>builder()
                .content(pageResult.getContent())
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional
    public PayoutRequestResponseDto processPayoutRequest(String payoutRequestId, PayoutRequestProcessDto dto) {
        PayoutRequest payoutRequest = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Không tìm thấy yêu cầu rút tiền: " + payoutRequestId));

        if (payoutRequest.getStatus() != PayoutRequestStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Yêu cầu rút tiền này đã được xử lý trước đó.");
        }

        PayoutRequestStatus status = PayoutRequestStatus.valueOf(dto.getStatus());
        if (status.equals(PayoutRequestStatus.REJECTED)) {
            // Trường hợp REJECTED: Hoàn trả số tiền về ví Campaign Wallet
            CampaignWallet wallet = campaignWalletRepository.findWithLockByAccountId(payoutRequest.getAccountId())
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Ví Campaign Wallet không tồn tại"));

            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(payoutRequest.getAmount());

            wallet.setBalance(balanceAfter);
            campaignWalletRepository.save(wallet);

            // Ghi vết hoàn tiền ví
            CampaignWalletTransaction refundTx = CampaignWalletTransaction.builder()
                    .campaignWallet(wallet)
                    .amount(payoutRequest.getAmount())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .transactionType(WalletTransactionType.PAYOUT_REFUND)
                    .referenceType(WalletReferenceType.PAYOUT_REQUEST)
                    .referenceId(payoutRequest.getPayoutRequestId())
                    .description("Hoàn tiền yêu cầu rút bị từ chối. Lý do: " + (dto.getAdminNote() != null ? dto.getAdminNote() : "N/A"))
                    .build();
            campaignWalletTransactionRepository.save(refundTx);

            payoutRequest.setStatus(PayoutRequestStatus.REJECTED);

        } else if (status.equals(PayoutRequestStatus.APPROVED)) {
            // Trường hợp APPROVED: Duyệt yêu cầu (tiền giữ ở 0 và chờ hệ thống Payout thực hiện chuyển tiền)
            payoutRequest.setStatus(PayoutRequestStatus.APPROVED);
        } else {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Trạng thái xử lý không hợp lệ.");
        }

        payoutRequest.setAdminNote(dto.getAdminNote());
        payoutRequestRepository.save(payoutRequest);

        return toResponseDto(payoutRequest);
    }

    @Override
    @Transactional
    public PayoutRequestResponseDto executePayout(String payoutRequestId) {
        // 1. Kiểm tra tồn tại PayoutRequest
        PayoutRequest payoutRequest = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new PaymentException(
                        PaymentErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy yêu cầu rút tiền với ID: " + payoutRequestId
                ));

        // 2. Ràng buộc: Chỉ những yêu cầu đã được APPROVED mới được tiến hành chi trả
        if (payoutRequest.getStatus() != PayoutRequestStatus.APPROVED) {
            throw new PaymentException(
                    PaymentErrorCode.ORDER_NOT_FOUND,
                    "Yêu cầu rút tiền chưa ở trạng thái APPROVED (Trạng thái hiện tại: " + payoutRequest.getStatus() + ")"
            );
        }

        long amountToPay = payoutRequest.getAmount() != null ? payoutRequest.getAmount().longValue() : 0L;
        if (amountToPay <= 0) {
            throw new PaymentException(
                    PaymentErrorCode.INSUFFICIENT_BALANCE,
                    "Số tiền chi trả phải lớn hơn 0"
            );
        }

        // 3. Chuẩn bị Batch Reference ID & PayoutItemRequestDto cho 1 phần tử
        String batchRefId = "WALLET_PAYOUT_BATCH_" + payoutRequestId + "_" + System.currentTimeMillis();

        PayoutItemRequestDto item = PayoutItemRequestDto.builder()
                .referenceId(payoutRequest.getPayoutRequestId())
                .amount(amountToPay)
                .description("Rut tien vi")
                .toBin(payoutRequest.getBankName() != null ? payoutRequest.getBankName().getBin() : "")
                .toAccountNumber(payoutRequest.getBankAccountNumber())
                .build();

        BatchPayoutRequestDto batchPayoutRequest = BatchPayoutRequestDto.builder()
                .referenceId(batchRefId)
                .validateDestination(true)
                .payouts(List.of(item))
                .build();

        // 4. Khởi tạo vết WalletPayoutTransaction ở trạng thái PENDING
        WalletPayoutTransaction payoutTxn = WalletPayoutTransaction.builder()
                .batchReferenceId(batchRefId)
                .transactionReferenceId(payoutRequest.getPayoutRequestId())
                .amount(payoutRequest.getAmount())
                .status(PayoutStatus.PENDING)
                .toBin(payoutRequest.getBankName())
                .toAccountNumber(payoutRequest.getBankAccountNumber())
                .toAccountName(payoutRequest.getBankAccountName())
                .payoutRequest(payoutRequest)
                .build();

        // 5. Gọi PayoutService thực hiện Batch Payout tới cổng PayOS
        try {
            BatchPayoutDataResponseDto gatewayResponse = payoutService.createBatchPayout(batchPayoutRequest);

            if (gatewayResponse != null && gatewayResponse.getTransactions() != null && !gatewayResponse.getTransactions().isEmpty()) {
                PayoutTransactionResponseDto gatewayTxn = gatewayResponse.getTransactions().getFirst();

                // Cập nhật WalletPayoutTransaction -> SUCCESS
                payoutTxn.setStatus(PayoutStatus.SUCCESS);
                payoutTxn.setGatewayBatchId(gatewayResponse.getId());
                payoutTxn.setPayoutReference(gatewayTxn.getId());
                payoutTxn.setPaidAt(LocalDateTime.now());
                walletPayoutTransactionRepository.save(payoutTxn);

                // Cập nhật PayoutRequest -> PAID
                payoutRequest.setStatus(PayoutRequestStatus.PAID);
                payoutRequestRepository.save(payoutRequest);

                log.info("Chi trả thành công cho PayoutRequest #{}. RefId: {}", payoutRequestId, gatewayTxn.getId());

            } else {
                // Gateway không trả về kết quả hợp lệ
                payoutTxn.setStatus(PayoutStatus.FAILED);
                payoutTxn.setFailureReason("Cổng thanh toán không trả về thông tin giao dịch hợp lệ");
                walletPayoutTransactionRepository.save(payoutTxn);

                throw new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Cổng thanh toán phản hồi không thành công.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện chi trả PayoutRequest #{}: {}", payoutRequestId, e.getMessage(), e);

            payoutTxn.setStatus(PayoutStatus.FAILED);
            payoutTxn.setFailureReason(e.getMessage());
            walletPayoutTransactionRepository.save(payoutTxn);

            throw new PaymentException(
                    PaymentErrorCode.ORDER_NOT_FOUND,
                    "Thực thi chuyển tiền qua cổng thanh toán thất bại: " + e.getMessage()
            );
        }

        return toResponseDto(payoutRequest);
    }

    private PayoutRequestResponseDto toResponseDto(PayoutRequest entity) {
        return PayoutRequestResponseDto.builder()
                .payoutRequestId(entity.getPayoutRequestId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paymentProfileId(entity.getPaymentProfileId())
                .bankName(entity.getBankName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankAccountName(entity.getBankAccountName())
                .adminNote(entity.getAdminNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}