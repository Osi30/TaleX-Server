package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.campaign.response.CampaignWalletBalanceDto;
import com.talex.server.dtos.campaign.response.CampaignWalletTransactionDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.CampaignWallet;
import com.talex.server.entities.campaign.CampaignWalletTransaction;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.engagement.WalletReferenceType;
import com.talex.server.enums.engagement.WalletTransactionType;
import com.talex.server.exceptions.codes.campaign.CampaignErrorCode;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.campaign.CampaignException;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.campaign.CampaignWalletRepository;
import com.talex.server.repositories.campaign.CampaignWalletTransactionRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.campaign.CampaignWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignWalletServiceImpl implements CampaignWalletService {

    private final CampaignWalletRepository campaignWalletRepository;
    private final CampaignWalletTransactionRepository campaignWalletTransactionRepository;
    private final CreatorRepository creatorRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public CampaignWallet getOrCreateWalletByAccountId(UUID accountId) {
        return campaignWalletRepository.findByCreator_Account_AccountId(accountId)
                .orElseGet(() -> {
                    Creator creator = creatorRepository.findByAccount_AccountId(accountId)
                            .orElseThrow(() -> new CampaignException(
                                    CampaignErrorCode.NOT_FOUND,
                                    "Không tìm thấy hồ sơ Creator cho tài khoản này"
                            ));

                    CampaignWallet newWallet = CampaignWallet.builder()
                            .creator(creator)
                            .balance(BigDecimal.ZERO)
                            .build();

                    return campaignWalletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public void refundCampaign(Campaign campaign) {
        // 1. Kiểm tra campaign có đơn hàng hợp lệ hay không
        if (campaign.getOrderId() == null || campaign.getOrderId().isBlank()) {
            return;
        }

        // 2. Lấy Order tương ứng để lấy giá trị thanh toán gốc (totalAmount)
        Order order = orderRepository.findById(campaign.getOrderId())
                .orElseThrow(() -> new CampaignException(
                        CampaignErrorCode.NOT_FOUND,
                        "Không tìm thấy thông tin đơn hàng với ID: " + campaign.getOrderId()
                ));

        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 3. Tính toán tỷ lệ dịch vụ chưa sử dụng để hoàn tiền
        long target = campaign.getTargetImpression() != null ? campaign.getTargetImpression() : 0L;
        long current = campaign.getCurrentImpression() != null ? campaign.getCurrentImpression() : 0L;

        BigDecimal refundAmount;
        if (target <= 0) {
            // Trường hợp target không xác định -> Hoàn lại 100%
            refundAmount = totalAmount;
        } else {
            long remainingImpressions = Math.max(0L, target - current);
            if (remainingImpressions == 0) {
                // Đã đạt 100% target -> Không hoàn tiền
                return;
            }

            // Tỷ lệ hoàn tiền = (Target - Current) / Target
            BigDecimal remainingRatio = BigDecimal.valueOf(remainingImpressions)
                    .divide(BigDecimal.valueOf(target), 4, RoundingMode.HALF_UP);

            refundAmount = totalAmount.multiply(remainingRatio);
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 4. Lấy hoặc tạo Ví Quảng Cáo cho Creator
        CampaignWallet wallet = getOrCreateWalletByAccountId(campaign.getAccountId());

        // 5. Cập nhật số dư trong ví
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(refundAmount);
        wallet.setBalance(balanceAfter);
        campaignWalletRepository.save(wallet);

        // 6. Ghi nhật ký giao dịch (CampaignWalletTransaction)
        CampaignWalletTransaction transaction = CampaignWalletTransaction.builder()
                .campaignWallet(wallet)
                .amount(refundAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionType(WalletTransactionType.REFUND)
                .referenceType(WalletReferenceType.CAMPAIGN)
                .referenceId(campaign.getCampaignId())
                .description("Hoàn tiền chiến dịch #" + campaign.getCampaignId() + " do bị hủy giữa chừng")
                .build();

        campaignWalletTransactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(UUID accountId) {
        return campaignWalletRepository.findByCreator_Account_AccountId(accountId)
                .map(CampaignWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignWalletBalanceDto getWalletBalanceDto(UUID accountId) {
        Optional<CampaignWallet> walletOpt = campaignWalletRepository.findByCreator_Account_AccountId(accountId);
        if (walletOpt.isEmpty()) {
            return null;
        }

        CampaignWallet wallet = walletOpt.get();
        return CampaignWalletBalanceDto.builder()
                .walletId(wallet.getWalletId())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void debitWallet(UUID accountId, BigDecimal amount, String description, String orderId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CampaignWallet wallet = campaignWalletRepository.findWithLockByAccountId(accountId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND, "Ví Campaign Wallet không tồn tại"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FULLY_COVERED_BY_COIN, "Số dư ví Campaign Wallet không đủ");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalance(balanceAfter);
        campaignWalletRepository.save(wallet);

        CampaignWalletTransaction tx = CampaignWalletTransaction.builder()
                .campaignWallet(wallet)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionType(WalletTransactionType.PAYMENT_DEDUCTION)
                .referenceType(WalletReferenceType.ORDER)
                .referenceId(orderId)
                .description(description)
                .build();

        campaignWalletTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void creditWallet(UUID accountId, BigDecimal amount, String description, String orderId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // Tự động tìm hoặc khởi tạo ví nếu chưa có
        CampaignWallet wallet = getOrCreateWalletByAccountId(accountId);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);
        campaignWalletRepository.save(wallet);

        CampaignWalletTransaction tx = CampaignWalletTransaction.builder()
                .campaignWallet(wallet)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionType(WalletTransactionType.REFUND)
                .referenceType(WalletReferenceType.ORDER)
                .referenceId(orderId)
                .description(description)
                .build();

        campaignWalletTransactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignWalletTransactionDto> getTransactionsByOrderId(String orderId) {
        return campaignWalletTransactionRepository
                .findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(WalletReferenceType.ORDER, orderId)
                .stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CampaignWalletTransactionDto> getWalletHistory(UUID accountId, Pageable pageable) {
        Optional<CampaignWallet> walletOpt = campaignWalletRepository.findByCreator_Account_AccountId(accountId);
        if (walletOpt.isEmpty()) {
            return BasePageResponse.<CampaignWalletTransactionDto>builder()
                    .content(Collections.emptyList())
                    .pageNumber(pageable.getPageNumber() + 1)
                    .pageSize(pageable.getPageSize())
                    .totalElements(0L)
                    .totalPages(0)
                    .isLast(true)
                    .build();
        }

        Page<CampaignWalletTransactionDto> page = campaignWalletTransactionRepository
                .findByCampaignWallet_WalletIdOrderByCreatedAtDesc(walletOpt.get().getWalletId(), pageable)
                .map(this::toTransactionDto);

        return BasePageResponse.<CampaignWalletTransactionDto>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    private CampaignWalletTransactionDto toTransactionDto(CampaignWalletTransaction tx) {
        return CampaignWalletTransactionDto.builder()
                .transactionId(tx.getTransactionId())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .transactionType(tx.getTransactionType())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}