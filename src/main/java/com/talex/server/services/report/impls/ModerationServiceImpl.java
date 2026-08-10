package com.talex.server.services.report.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.TicketProcessRequestDto;
import com.talex.server.dtos.report.response.PenaltyResponseDto;
import com.talex.server.dtos.report.response.TicketResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.entities.report.Penalty;
import com.talex.server.entities.report.Report;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.report.*;
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
import com.talex.server.services.report.ModerationService;
import com.talex.server.specifications.report.ModerationTicketSpec;
import com.talex.server.utils.PageUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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

        questDBSender.table("report_logs")
                .symbol("ticket_id", ticketId)
                .symbol("actor_id", staffId)
                .symbol("report_role", role)
                .symbol("action_type", AuditActionType.TICKET_ASSIGNED.toString())
                .symbol("target_type", ticket.getTargetType().toString())
                .symbol("target_id", ticket.getTargetId())
                .symbol("payload", "Assigned ticket: " + ticketId)
                .at(Instant.now());

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

            questDBSender.table("report_logs")
                    .symbol("ticket_id", ticketId)
                    .symbol("actor_id", staffId)
                    .symbol("report_role", role)
                    .symbol("action_type", AuditActionType.TICKET_DISMISSED.toString())
                    .symbol("target_type", ticket.getTargetType().toString())
                    .symbol("target_id", ticket.getTargetId())
                    .symbol("payload", "Dismissed ticket with reason: " + requestDto.getReason())
                    .at(Instant.now());

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
            publishPenaltyEvent(savedPenalty);
        }

        // 3. Tự động kiểm tra và leo thang hình phạt (Warning/Fine Escalation Logic)
        applyPenaltyEscalationRules(savedPenalty, ticket.getTargetId(), staffId);

        // 4. Ghi Audit Log
        questDBSender.table("report_logs")
                .symbol("ticket_id", ticketId)
                .symbol("actor_id", staffId)
                .symbol("report_role", role)
                .symbol("action_type", AuditActionType.PENALTY_ISSUED.toString())
                .symbol("target_type", ticket.getTargetType().toString())
                .symbol("target_id", ticket.getTargetId())
                .symbol("payload", "Issued penalty level: " + requestDto.getPenaltyLevel())
                .at(Instant.now());

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

    /**
     * Logic Leo thang hình phạt dựa trên Cảnh cáo (Warning) hoặc Phạt thật (Fine)
     */
    private void applyPenaltyEscalationRules(Penalty currentPenalty, String targetId, String staffId) {
        PenaltyLevel level = currentPenalty.getLevel();
        String userId = currentPenalty.getTargetUserId();

        if (isWarningLevel(level)) {
            // Nếu người xử lý chọn Cảnh cáo (Warning) -> Kiểm tra số đợt tích lũy
            checkAndEscalateWarning(userId, level, targetId, staffId);
        } else if (isFineLevel(level)) {
            // Nếu người xử lý đánh Gậy phạt trực tiếp (Fine) -> Phát 1 Cảnh cáo cấp cao hơn (nếu có)
            issueHigherLevelWarning(userId, level, targetId, staffId);
        }
    }

    // Xử Lý Cảnh Báo
    private void checkAndEscalateWarning(String userId, PenaltyLevel warningLevel, String targetId, String staffId) {
        long warningCount = penaltyRepository.countByTargetUserIdAndStatusAndLevelIn(
                userId, PenaltyStatus.ACTIVE, List.of(warningLevel));

        if (warningCount < 3) {
            return; // Chưa đạt mốc 3 lần cảnh cáo
        }

        // Khi đủ >= 3 Cảnh cáo -> Tự động leo thang hình phạt
        switch (warningLevel) {
            case WARNING_COMMENT -> {
                // 3 Warning Comment -> Phạt thẳng FINE_ACCOUNT
                createAutoPenalty(userId, PenaltyLevel.FINE_ACCOUNT, TargetType.ACCOUNT, userId,
                        "Tài khoản bị khóa tự động do tích lũy đủ 3 lần cảnh cáo ở Bình luận", staffId);
            }
            case WARNING_EPISODE -> {
                // 3 Warning Episode -> FINE Episode + 1 WARNING Series
                createAutoPenalty(userId, PenaltyLevel.FINE_EPISODE, TargetType.EPISODE, targetId,
                        "Tập phim bị phạt tự động do tích lũy đủ 3 lần cảnh cáo", staffId);

                createAutoPenalty(userId, PenaltyLevel.WARNING_SERIES, TargetType.SERIES, targetId,
                        "Cảnh cáo cấp độ Series do Tập phim bị phạt đủ 3 lần", staffId);

                // Tự động kiểm tra tiếp xem Series đã tích đủ 3 Warning chưa
                checkAndEscalateWarning(userId, PenaltyLevel.WARNING_SERIES, targetId, staffId);
            }
            case WARNING_SERIES -> {
                // 3 Warning Series -> FINE Series + 1 WARNING Account
                createAutoPenalty(userId, PenaltyLevel.FINE_SERIES, TargetType.SERIES, targetId,
                        "Series bị phạt tự động do tích lũy đủ 3 lần cảnh cáo", staffId);

                createAutoPenalty(userId, PenaltyLevel.WARNING_ACCOUNT, TargetType.ACCOUNT, userId,
                        "Cảnh cáo cấp độ Tài khoản do Series bị phạt đủ 3 lần", staffId);

                // Tự động kiểm tra tiếp xem Account đã tích đủ 3 Warning chưa
                checkAndEscalateWarning(userId, PenaltyLevel.WARNING_ACCOUNT, userId, staffId);
            }
            case WARNING_ACCOUNT -> {
                // 3 Warning Account -> FINE Account
                createAutoPenalty(userId, PenaltyLevel.FINE_ACCOUNT, TargetType.ACCOUNT, userId,
                        "Tài khoản bị khóa tự động do tích lũy đủ 3 lần cảnh cáo cấp Tài khoản", staffId);
            }
            default -> {}
        }
    }

    // Xử Lý Phạt
    private void issueHigherLevelWarning(String userId, PenaltyLevel fineLevel, String targetId, String staffId) {
        switch (fineLevel) {
            case FINE_EPISODE -> {
                // Đánh gậy Episode -> Bỏ qua check warning episode, phát 1 WARNING Series
                createAutoPenalty(userId, PenaltyLevel.WARNING_SERIES, TargetType.SERIES, targetId,
                        "Cảnh cáo cấp độ Series do Tập phim bị phạt gậy trực tiếp", staffId);

                // Kiểm tra xem WARNING Series vừa phát có chạm mốc 3 không
                checkAndEscalateWarning(userId, PenaltyLevel.WARNING_SERIES, targetId, staffId);
            }
            case FINE_SERIES -> {
                // Đánh gậy Series -> Phát 1 WARNING Account
                createAutoPenalty(userId, PenaltyLevel.WARNING_ACCOUNT, TargetType.ACCOUNT, userId,
                        "Cảnh cáo cấp độ Tài khoản do Series bị phạt gậy trực tiếp", staffId);

                // Kiểm tra xem WARNING Account vừa phát có chạm mốc 3 không
                checkAndEscalateWarning(userId, PenaltyLevel.WARNING_ACCOUNT, userId, staffId);
            }
            case FINE_ACCOUNT -> {
                // Cấp cuối cùng -> Không còn warning cấp cao hơn
            }
            default -> {}
        }
    }

    private void createAutoPenalty(String userId, PenaltyLevel level, TargetType targetType, String targetId, String reason, String staffId) {
        Penalty autoPenalty = Penalty.builder()
                .targetUserId(userId)
                .issuerId("SYSTEM_AUTO (" + staffId + ")")
                .level(level)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .status(PenaltyStatus.ACTIVE)
                .build();
        autoPenalty = penaltyRepository.save(autoPenalty);
        if (isFineLevel(level)) {
            publishPenaltyEvent(autoPenalty);
        }
    }

    private boolean isWarningLevel(PenaltyLevel level) {
        return level == PenaltyLevel.WARNING_COMMENT ||
                level == PenaltyLevel.WARNING_EPISODE ||
                level == PenaltyLevel.WARNING_SERIES ||
                level == PenaltyLevel.WARNING_ACCOUNT;
    }

    private boolean isFineLevel(PenaltyLevel level) {
        return level == PenaltyLevel.FINE_EPISODE ||
                level == PenaltyLevel.FINE_SERIES ||
                level == PenaltyLevel.FINE_ACCOUNT;
    }

    private void publishPenaltyEvent(Penalty penalty) {
        try {
            String payload = objectMapper.writeValueAsString(penalty);
            kafkaTemplate.send("penalty-event-topic", penalty.getTargetUserId(), payload);
        } catch (Exception e) {
            log.error("Lỗi khi gửi Kafka message cho PenaltyId: {}", penalty.getPenaltyId(), e);
        }
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