package com.talex.server.services.report.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.AppealProcessRequestDto;
import com.talex.server.dtos.report.request.AppealRequestDto;
import com.talex.server.dtos.report.response.AppealResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.report.Appeal;
import com.talex.server.entities.report.Penalty;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.report.AppealStatus;
import com.talex.server.enums.report.AuditActionType;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import com.talex.server.exceptions.details.report.ModerationException;
import com.talex.server.mappers.report.AppealMapper;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.report.AppealRepository;
import com.talex.server.repositories.report.PenaltyRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.auth.AdminAccountService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.report.AppealService;
import com.talex.server.services.series.EpisodeService;
import com.talex.server.services.series.SeriesService;
import com.talex.server.specifications.report.AppealSpec;
import com.talex.server.utils.PageUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final PenaltyRepository penaltyRepository;
    private final NotificationRepository notificationRepository;
    private final AppealMapper appealMapper;
    private final Sender questDBSender;

    private final EpisodeService episodeService;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final CreatorService creatorService;
    private final SeriesService seriesService;
    private final AdminAccountService adminAccountService;

    @Override
    @Transactional
    public AppealResponseDto createAppeal(String currentUserId, String role, String penaltyId, AppealRequestDto requestDto) {
        Penalty penalty = penaltyRepository.findByPenaltyIdAndStatus(penaltyId, PenaltyStatus.ACTIVE)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.PENALTY_NOT_FOUND));

        // 1. Check xem đã từng khiếu nại chưa hoặc đã hết hạn xử lý
        validateAppeal(penalty);

        // 2. Tạo Appeal Entity
        Appeal appeal = Appeal.builder()
                .penalty(penalty)
                .appellantId(currentUserId)
                .reason(requestDto.getReason())
                .proofDocuments(requestDto.getProofDocuments())
                .status(AppealStatus.PENDING)
                .build();
        Appeal savedAppeal = appealRepository.save(appeal);

        // 3. Ghi Audit Log
        questDBSender.table("penalty_logs")
                .symbol("penalty_id", penaltyId)
                .symbol("actor_id", currentUserId)
                .symbol("report_role", role)
                .symbol("action_type", AuditActionType.APPEAL_SUBMITTED.toString())
                .symbol("target_type", penalty.getTargetType().toString())
                .symbol("target_id", penalty.getTargetId())
                .symbol("payload", "Submitted appeal for penalty: " + penaltyId)
                .at(Instant.now());

        return appealMapper.toResponseDto(savedAppeal);
    }

    /// Check xem đã từng khiếu nại chưa
    private void validateAppeal(Penalty penalty){
        if (appealRepository.existsByPenalty_PenaltyId(penalty.getPenaltyId())) {
            throw new ModerationException(ModerationErrorCode.ALREADY_APPEALED);
        }

    }

    @Override
    @Transactional
    public AppealResponseDto processAppeal(String adminId, String role, String appealId, AppealProcessRequestDto requestDto) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.APPEAL_NOT_FOUND));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ModerationException(ModerationErrorCode.INVALID_STATUS);
        }

        appeal.setReviewerId(adminId);
        appeal.setAdminNote(requestDto.getAdminNote());
        appeal.setResolvedAt(LocalDateTime.now());

        Penalty penalty = appeal.getPenalty();

        if (requestDto.getIsApproved()) {
            // Admin Chấp nhận Khiếu nại -> Gỡ phạt (Revoke Penalty)
            appeal.setStatus(AppealStatus.APPROVED);
            penalty.setStatus(PenaltyStatus.REVOKED);
            penaltyRepository.save(penalty);
            processPenalty(penalty, adminId);

            questDBSender.table("penalty_logs")
                    .symbol("penalty_id", penalty.getPenaltyId())
                    .symbol("actor_id", adminId)
                    .symbol("report_role", role)
                    .symbol("action_type", AuditActionType.APPEAL_APPROVED.toString())
                    .symbol("target_type", penalty.getTargetType().toString())
                    .symbol("target_id", penalty.getTargetId())
                    .symbol("payload", "Approved appeal. Penalty revoked.")
                    .at(Instant.now());

            notificationRepository.save(Notification.builder()
                    .recipientId(appeal.getAppellantId())
                    .title("Khiếu nại được chấp nhận")
                    .content("Đơn khiếu nại của bạn đã được Admin chấp nhận. Hình phạt đã được gỡ bỏ.")
                    .type(NotificationType.APPEAL_RESULT)
                    .referenceType("APPEAL")
                    .referenceId(appealId)
                    .build());
        } else {
            // Admin Bác bỏ Khiếu nại -> Giữ nguyên phạt
            appeal.setStatus(AppealStatus.REJECTED);

            questDBSender.table("penalty_logs")
                    .symbol("penalty_id", penalty.getPenaltyId())
                    .symbol("actor_id", adminId)
                    .symbol("report_role", role)
                    .symbol("action_type", AuditActionType.APPEAL_REJECTED.toString())
                    .symbol("target_type", penalty.getTargetType().toString())
                    .symbol("target_id", penalty.getTargetId())
                    .symbol("payload", "Rejected appeal with note: " + requestDto.getAdminNote())
                    .at(Instant.now());

            notificationRepository.save(Notification.builder()
                    .recipientId(appeal.getAppellantId())
                    .title("Khiếu nại bị từ chối")
                    .content("Đơn khiếu nại của bạn đã bị từ chối. Lý do: " + requestDto.getAdminNote())
                    .type(NotificationType.APPEAL_RESULT)
                    .referenceType("APPEAL")
                    .referenceId(appealId)
                    .build());
        }

        Appeal updatedAppeal = appealRepository.save(appeal);
        return appealMapper.toResponseDto(updatedAppeal);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<AppealResponseDto> filterAppeals(BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Appeal> pageResult = appealRepository.findAll(
                AppealSpec.filterByCriteria(filterRequest.getCriteria()), pageable);

        List<AppealResponseDto> content = pageResult.stream().map(appealMapper::toResponseDto).toList();

        return BasePageResponse.<AppealResponseDto>builder()
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
    public BasePageResponse<AppealResponseDto> filterAppeals(String accountId, BaseFilterRequestDto filterRequest) {
        filterRequest.getCriteria().put("appellantId", accountId);
        return filterAppeals(filterRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public AppealResponseDto getAppealByPenaltyId(String penaltyId) {
        Appeal appeal = appealRepository.findByPenalty_PenaltyId(penaltyId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.APPEAL_NOT_FOUND));

        return appealMapper.toResponseDto(appeal);
    }

    private void processPenalty(Penalty penalty, String adminId) {
        if (penalty == null || penalty.getTargetType() == null) {
            return;
        }

        switch (penalty.getTargetType()) {
            case EPISODE -> processEpisode(penalty, adminId);
            case SERIES -> processSeries(penalty, adminId);
            case ACCOUNT -> processAccount(penalty);
            default -> log.warn("Chưa hỗ trợ gỡ phạt tự động cho TargetType: {}", penalty.getTargetType());
        }
    }

    private void processEpisode(Penalty penalty, String adminId) {
        String episodeId = penalty.getTargetId();

        // 1. Force unhide Episode
        episodeService.forceUnhide(episodeId, adminId);

        // 2. Tìm kiếm giao dịch khấu trừ tiền phạt trước đó
        revenueTransactionRepository
                .findByReferenceTypeAndReferenceIdAndRevenueTransactionType(
                        ReferenceType.PENALTY,
                        penalty.getPenaltyId(),
                        RevenueTransactionType.PENALTY_DEDUCTION)
                .ifPresent(deductionTx -> {
                    BigDecimal fineAmount = deductionTx.getAmount();
                    Creator creator = deductionTx.getCreator();
                    String creatorId = creator.getCreatorId();

                    BigDecimal balanceBefore = creator.getCurrentBalance() != null
                            ? creator.getCurrentBalance()
                            : BigDecimal.ZERO;
                    BigDecimal balanceAfter = balanceBefore.add(fineAmount);

                    String description = String.format(
                            "Hoàn trả tiền phạt cho Episode %s do chấp nhận khiếu nại (Án phạt %s): Số tiền hoàn: %s VNĐ.",
                            episodeId,
                            penalty.getPenaltyId(),
                            fineAmount.toPlainString()
                    );

                    RevenueTransaction refundTx = RevenueTransaction.builder()
                            .amount(fineAmount)
                            .balanceBefore(balanceBefore)
                            .balanceAfter(balanceAfter)
                            .revenueTransactionType(RevenueTransactionType.ADJUSTMENT)
                            .referenceType(ReferenceType.APPEAL)
                            .referenceId(penalty.getPenaltyId())
                            .description(description)
                            .creator(creator)
                            .creatorMonthlySettlement(null)
                            .monthYear(LocalDate.now())
                            .build();

                    revenueTransactionRepository.save(refundTx);

                    // 3. Cộng lại tiền vào ví Creator
                    creatorService.updateBalance(creatorId, fineAmount);
                });
    }

    private void processSeries(Penalty penalty, String adminId) {
        String seriesId = penalty.getTargetId();

        // 1. Force unhide Series
        seriesService.forceUnhide(seriesId, adminId);

        // 2. Tìm kiếm giao dịch khấu trừ tiền phạt trước đó
        revenueTransactionRepository
                .findByReferenceTypeAndReferenceIdAndRevenueTransactionType(
                        ReferenceType.PENALTY,
                        penalty.getPenaltyId(),
                        RevenueTransactionType.PENALTY_DEDUCTION)
                .ifPresent(deductionTx -> {
                    BigDecimal fineAmount = deductionTx.getAmount();
                    Creator creator = deductionTx.getCreator();
                    String creatorId = creator.getCreatorId();

                    BigDecimal balanceBefore = creator.getCurrentBalance() != null
                            ? creator.getCurrentBalance()
                            : BigDecimal.ZERO;
                    BigDecimal balanceAfter = balanceBefore.add(fineAmount);

                    String description = String.format(
                            "Hoàn trả tiền phạt cho Series %s do chấp nhận khiếu nại (Án phạt %s): Số tiền hoàn: %s VNĐ.",
                            seriesId,
                            penalty.getPenaltyId(),
                            fineAmount.toPlainString()
                    );

                    RevenueTransaction refundTx = RevenueTransaction.builder()
                            .amount(fineAmount)
                            .balanceBefore(balanceBefore)
                            .balanceAfter(balanceAfter)
                            .revenueTransactionType(RevenueTransactionType.ADJUSTMENT)
                            .referenceType(ReferenceType.APPEAL)
                            .referenceId(penalty.getPenaltyId())
                            .description(description)
                            .creator(creator)
                            .creatorMonthlySettlement(null)
                            .monthYear(LocalDate.now())
                            .build();

                    revenueTransactionRepository.save(refundTx);

                    // 3. Cộng lại tiền vào ví Creator
                    creatorService.updateBalance(creatorId, fineAmount);
                });
    }

    private void processAccount(Penalty penalty) {
        String accountIdStr = penalty.getTargetId();
        adminAccountService.unbanAccount(UUID.fromString(accountIdStr));
    }
}