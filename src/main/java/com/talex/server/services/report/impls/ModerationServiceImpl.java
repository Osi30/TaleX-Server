package com.talex.server.services.report.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.TicketProcessRequestDto;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.dtos.report.response.TicketResponseDto;
import com.talex.server.dtos.settlement.episode.TotalEpisodeRevenueDto;
import com.talex.server.dtos.settlement.series.TotalSeriesRevenueDto;
import com.talex.server.entities.Notification;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.entities.report.Penalty;
import com.talex.server.entities.report.Report;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.report.*;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import com.talex.server.exceptions.details.report.ModerationException;
import com.talex.server.mappers.report.ModerationTicketMapper;
import com.talex.server.mappers.report.PenaltyMapper;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.interaction.AccountCommentRepository;
import com.talex.server.repositories.report.ModerationTicketRepository;
import com.talex.server.repositories.report.PenaltyRepository;
import com.talex.server.repositories.report.ReportRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.auth.AdminAccountService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.creator.RevenueTransactionService;
import com.talex.server.services.interaction.AccountCommentService;
import com.talex.server.services.report.ModerationService;
import com.talex.server.services.series.EpisodeService;
import com.talex.server.services.series.SeriesService;
import com.talex.server.specifications.report.ModerationTicketSpec;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationServiceImpl implements ModerationService {

    private final ModerationTicketRepository ticketRepository;
    private final ReportRepository reportRepository;
    private final PenaltyRepository penaltyRepository;
    private final NotificationRepository notificationRepository;
    private final AccountCommentRepository accountCommentRepository;
    private final EpisodeRepository episodeRepository;
    private final SeriesRepository seriesRepository;
    private final AccountRepository accountRepository;
    private final ModerationTicketMapper ticketMapper;
    private final PenaltyMapper penaltyMapper;
    private final Sender questDBSender;

    private final EpisodeService episodeService;
    private final SeriesService seriesService;
    private final AdminAccountService adminAccountService;
    private final AccountCommentService accountCommentService;
    private final RevenueTransactionService revenueTransactionService;
    private final CreatorService creatorService;
    private final RevenueTransactionRepository revenueTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(String ticketId) {
        ModerationTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.TICKET_NOT_FOUND));

        return ticketMapper.toResponseDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<TicketResponseDto> filterTickets(BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(),
                Sort.by(Sort.Direction.DESC, "priorityScore").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Page<ModerationTicket> pageResult = ticketRepository.findAll(
                ModerationTicketSpec.filterByCriteria(filterRequest.getCriteria()), pageable);

        List<TicketResponseDto> content = pageResult.stream().map(ticketMapper::toResponseDto).toList();

        return BasePageResponse.<TicketResponseDto>builder()
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
    @Transactional
    public TicketResponseDto assignTicketToStaff(String ticketId, String role, String staffId) {
        ModerationTicket ticket = ticketRepository.findByTicketIdAndStatusIs(ticketId, TicketStatus.OPEN)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.TICKET_NOT_FOUND));

        ticket.setAssignedStaffId(staffId);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ModerationTicket updated = ticketRepository.save(ticket);

//        questDBSender.table("report_logs")
//                .symbol("ticket_id", ticketId)
//                .symbol("actor_id", staffId)
//                .symbol("report_role", role)
//                .symbol("action_type", AuditActionType.TICKET_ASSIGNED.toString())
//                .symbol("target_type", ticket.getTargetType().toString())
//                .symbol("target_id", ticket.getTargetId())
//                .symbol("payload", "Assigned ticket: " + ticketId)
//                .at(Instant.now());

        return ticketMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public PenaltyResponseDto processTicket(String ticketId, String staffId, String role, TicketProcessRequestDto requestDto) {
        ModerationTicket ticket = ticketRepository.findByTicketIdAndStatusIsAndAssignedStaffId(ticketId, TicketStatus.IN_PROGRESS, staffId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.TICKET_NOT_FOUND));

        // 1. Nếu bác bỏ báo cáo (Dismiss Ticket)
        if (!requestDto.getIsApproved()) {
            ticket.setStatus(TicketStatus.DISMISSED);
            ticketRepository.save(ticket);

            // Cập nhật trạng thái các Báo cáo con
            List<Report> reports = reportRepository.findByModerationTicket_TicketId(ticketId);
            reports.forEach(r -> r.setStatus(ReportStatus.REJECTED));
            reportRepository.saveAll(reports);

//            questDBSender.table("report_logs")
//                    .symbol("ticket_id", ticketId)
//                    .symbol("actor_id", staffId)
//                    .symbol("report_role", role)
//                    .symbol("action_type", AuditActionType.TICKET_DISMISSED.toString())
//                    .symbol("target_type", ticket.getTargetType().toString())
//                    .symbol("target_id", ticket.getTargetId())
//                    .symbol("payload", "Dismissed ticket with reason: " + requestDto.getReason())
//                    .at(Instant.now());

            return null;
        }

        // 2. Nếu chấp nhận báo cáo -> Ban hành Đánh Gậy (Penalty)
        ticket.setStatus(TicketStatus.RESOLVED);
        ticketRepository.save(ticket);

        List<Report> reports = reportRepository.findByModerationTicket_TicketId(ticketId);
        reports.forEach(r -> r.setStatus(ReportStatus.RESOLVED));
        reportRepository.saveAll(reports);

        String ownerUserId = resolveOwnerUserId(ticket.getTargetType(), ticket.getTargetId());

        Penalty penalty = Penalty.builder()
                .ticketId(ticketId)
                .targetUserId(ownerUserId)
                .issuerId(staffId)
                .level(requestDto.getPenaltyLevel())
                .targetType(ticket.getTargetType())
                .targetId(ticket.getTargetId())
                .reason(requestDto.getReason())
                .status(PenaltyStatus.ACTIVE)
                .build();
        Penalty savedPenalty = penaltyRepository.save(penalty);
        if (isFineLevel(requestDto.getPenaltyLevel())) {
            processPenalty(savedPenalty, staffId);
        }

        // 4. Ghi Audit Log
//        questDBSender.table("report_logs")
//                .symbol("ticket_id", ticketId)
//                .symbol("actor_id", staffId)
//                .symbol("report_role", role)
//                .symbol("action_type", AuditActionType.PENALTY_ISSUED.toString())
//                .symbol("target_type", ticket.getTargetType().toString())
//                .symbol("target_id", ticket.getTargetId())
//                .symbol("payload", "Issued penalty level: " + requestDto.getPenaltyLevel())
//                .at(Instant.now());

        // 5. Gửi Thông Báo cho người bị phạt
        notificationRepository.save(Notification.builder()
                .recipientId(ownerUserId)
                .title("Thông báo xử phạt vi phạm")
                .content("Nội dung (" + ticket.getTargetType() + ") của bạn bị xử phạt: " + requestDto.getReason())
                .type(NotificationType.PENALTY_WARNING)
                .referenceType("PENALTY")
                .referenceId(savedPenalty.getPenaltyId())
                .build());

        return penaltyMapper.toResponseDto(savedPenalty);
    }

    private boolean isFineLevel(PenaltyLevel level) {
        return level == PenaltyLevel.FINE_EPISODE ||
                level == PenaltyLevel.FINE_SERIES ||
                level == PenaltyLevel.FINE_ACCOUNT;
    }

    private void processPenalty(Penalty penalty, String staffId) {
        if (penalty == null || penalty.getTargetType() == null) {
            return;
        }

        switch (penalty.getTargetType()) {
            case EPISODE -> processEpisode(penalty, staffId);
            case SERIES -> processSeries(penalty, staffId);
            case ACCOUNT -> processAccount(penalty);
            case COMMENT -> processComment(penalty);
            default -> log.warn("Chưa hỗ trợ xử lý phạt tự động cho TargetType: {}", penalty.getTargetType());
        }
    }

    private void processEpisode(Penalty penalty, String staffId) {
        String episodeId = penalty.getTargetId();

        // 1. Gọi forceHide Episode
        episodeService.forceHide(episodeId, staffId);

        // 2. Lấy số tiền chưa quyết toán (Mua lẻ + Premium)
        TotalEpisodeRevenueDto unsettledRevenue = revenueTransactionService.getTotalUnsettledRevenueByEpisodeId(episodeId);
        BigDecimal directFine = unsettledRevenue.getUnsettledDirectAmount();
        BigDecimal subFine = unsettledRevenue.getUnsettledSubscriptionAmount();
        BigDecimal totalFineAmount = unsettledRevenue.getTotalUnsettledAmount();

        if (totalFineAmount.equals(BigDecimal.ZERO)) return;

        // 3. Lấy thông tin Creator
        String creatorId = episodeService.getCreatorIdByEpisodeId(episodeId);
        Creator creatorEntity = creatorService.getEntityById(creatorId);

        // 4. Tính toán số dư ví trước và sau khi trừ phạt
        BigDecimal balanceBefore = creatorEntity.getCurrentBalance() != null
                ? creatorEntity.getCurrentBalance()
                : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.subtract(totalFineAmount);

        // 5. Tạo mới RevenueTransaction khấu trừ tiền phạt
        String description = String.format(
                "Khấu trừ phạt vi phạm cho Episode %s (Án phạt %s): Tiền phạt mua lẻ: %s VNĐ, Tiền phạt chia gói Premium: %s VNĐ. Tổng trừ: %s VNĐ.",
                episodeId,
                penalty.getPenaltyId(),
                directFine.toPlainString(),
                subFine.toPlainString(),
                totalFineAmount.toPlainString()
        );

        RevenueTransaction penaltyTransaction = RevenueTransaction.builder()
                .amount(totalFineAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .revenueTransactionType(RevenueTransactionType.PENALTY_DEDUCTION)
                .referenceType(ReferenceType.PENALTY)
                .referenceId(penalty.getPenaltyId())
                .description(description)
                .creator(creatorEntity)
                .creatorMonthlySettlement(null)
                .monthYear(LocalDate.now())
                .build();

        revenueTransactionRepository.save(penaltyTransaction);

        // 6. Trừ số dư thực tế của Creator
        creatorService.updateBalance(creatorId, totalFineAmount.negate());
    }

    private void processSeries(Penalty penalty, String staffId) {
        String seriesId = penalty.getTargetId();

        // 1. Force hide Series
        seriesService.forceHide(seriesId, staffId);

        // 2. Lấy tổng doanh thu chưa quyết toán của tất cả các Episode thuộc Series này
        TotalSeriesRevenueDto unsettledRevenue = revenueTransactionService.getTotalUnsettledRevenueBySeriesId(seriesId);
        BigDecimal directFine = unsettledRevenue.getUnsettledDirectAmount();
        BigDecimal subFine = unsettledRevenue.getUnsettledSubscriptionAmount();
        BigDecimal totalFineAmount = unsettledRevenue.getTotalUnsettledAmount();

        if (totalFineAmount.compareTo(BigDecimal.ZERO) == 0) return;

        // 3. Lấy Creator của Series
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ModerationException(ModerationErrorCode.TARGET_NOT_FOUND));
        Creator creatorEntity = series.getCreator();
        String creatorId = creatorEntity.getCreatorId();

        // 4. Tính toán số dư ví trước và sau khi phạt
        BigDecimal balanceBefore = creatorEntity.getCurrentBalance() != null
                ? creatorEntity.getCurrentBalance()
                : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.subtract(totalFineAmount);

        // 5. Tạo giao dịch khấu trừ phạt (PENALTY_DEDUCTION)
        String description = String.format(
                "Khấu trừ phạt vi phạm cho Series %s (Án phạt %s): Tiền phạt mua lẻ: %s VNĐ, Tiền phạt chia gói Premium: %s VNĐ. Tổng trừ: %s VNĐ.",
                seriesId,
                penalty.getPenaltyId(),
                directFine.toPlainString(),
                subFine.toPlainString(),
                totalFineAmount.toPlainString()
        );

        RevenueTransaction penaltyTransaction = RevenueTransaction.builder()
                .amount(totalFineAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .revenueTransactionType(RevenueTransactionType.PENALTY_DEDUCTION)
                .referenceType(ReferenceType.PENALTY)
                .referenceId(penalty.getPenaltyId())
                .description(description)
                .creator(creatorEntity)
                .creatorMonthlySettlement(null)
                .monthYear(LocalDate.now())
                .build();

        revenueTransactionRepository.save(penaltyTransaction);

        // 6. Cập nhật số dư thực tế của Creator
        creatorService.updateBalance(creatorId, totalFineAmount.negate());
    }

    private void processAccount(Penalty penalty) {
        String accountIdStr = penalty.getTargetId();
        adminAccountService.banAccount(UUID.fromString(accountIdStr));
    }

    private void processComment(Penalty penalty) {
        String commentId = penalty.getTargetId();
        accountCommentService.hideCommentByAdmin(commentId);
    }

    /**
     * Suy ra Account ID của chủ sở hữu đối tượng bị báo cáo
     */
    private String resolveOwnerUserId(TargetType targetType, String targetId) {
        if (targetType == null || targetId == null) {
            throw new ModerationException(ModerationErrorCode.INVALID_TARGET);
        }

        return switch (targetType) {
            case COMMENT -> accountCommentRepository.findById(targetId)
                    .map(comment -> comment.getAccount().getAccountId().toString())
                    .orElseThrow(() -> new ModerationException(ModerationErrorCode.TARGET_NOT_FOUND));

            case EPISODE -> episodeRepository.findById(targetId)
                    .map(episode -> episode.getSeason().getSeries().getCreator().getAccount().getAccountId().toString())
                    .orElseThrow(() -> new ModerationException(ModerationErrorCode.TARGET_NOT_FOUND));

            case SERIES -> seriesRepository.findById(targetId)
                    .map(series -> series.getCreator().getAccount().getAccountId().toString())
                    .orElseThrow(() -> new ModerationException(ModerationErrorCode.TARGET_NOT_FOUND));

            case ACCOUNT -> accountRepository.findById(UUID.fromString(targetId))
                    .map(account -> account.getAccountId().toString())
                    .orElseThrow(() -> new ModerationException(ModerationErrorCode.TARGET_NOT_FOUND));

            default -> throw new ModerationException(ModerationErrorCode.INVALID_TARGET_TYPE);
        };
    }
}