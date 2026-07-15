package com.talex.server.workers.interaction;

import com.talex.server.entities.auth.Role;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import com.talex.server.enums.creator.PaymentProfileStatus;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.records.CreatorVerificationStatus;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.auth.IRoleService;
import com.talex.server.services.creator.ICreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountWorker {
    private final ICreatorService creatorService;
    private final AccountRepository accountRepository;
    private final IRoleService roleService;

    @KafkaListener(topics = "request-to-update-account", groupId = "creator-role-updater-group")
    @Transactional
    public void consumeAccountUpdateRequest(String accountIdStr) {
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            CreatorVerificationStatus statusDto = creatorService.checkAndGetVerificationStatus(accountId);
            boolean isEligibleForCreatorRole =
                    Boolean.TRUE.equals(statusDto.isCreatorVerified()) &&
                            Boolean.TRUE.equals(statusDto.isTermsAccepted()) &&
                            CreatorIdentityStatus.APPROVED.toString().equalsIgnoreCase(statusDto.identityStatus()) &&
                            PaymentProfileStatus.VERIFIED.toString().equalsIgnoreCase(statusDto.paymentStatus());

            if (isEligibleForCreatorRole) {
                accountRepository.findById(accountId).ifPresentOrElse(account -> {
                    Role role = roleService.findByCode("CREATOR");
                    account.setRole(role);
                    accountRepository.save(account);
                    }, () -> log.error("[Kafka Consumer] Không tìm thấy Account trong DB ứng với ID: {}", accountId));
            } else {
                log.warn("[Kafka Consumer] Tài khoản {} chưa đủ điều kiện (Identity/Payment chưa ở trạng thái APPROVED/VERIFIED).", accountId);
            }

        } catch (CreatorException e) {
            log.warn("[Kafka Consumer] Ngắt mạch xử lý nâng role cho {}. Lý do hệ thống từ chối: {}", accountIdStr, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[Kafka Consumer] Định dạng UUID gửi từ Kafka không hợp lệ: {}", accountIdStr);
        } catch (Exception e) {
            log.error("[Kafka Consumer] Lỗi hệ thống khi xử lý nâng role cho accountId: {}", accountIdStr, e);
        }
    }
}
