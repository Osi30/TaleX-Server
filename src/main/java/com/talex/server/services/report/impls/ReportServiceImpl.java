package com.talex.server.services.report.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;
import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.entities.report.Report;
import com.talex.server.enums.report.AuditActionType;
import com.talex.server.enums.report.ReportReason;
import com.talex.server.enums.report.ReportStatus;
import com.talex.server.enums.report.TicketStatus;
import com.talex.server.exceptions.codes.report.ModerationErrorCode;
import com.talex.server.exceptions.details.report.ModerationException;
import com.talex.server.mappers.report.IReportMapper;
import com.talex.server.repositories.report.ModerationTicketRepository;
import com.talex.server.repositories.report.ReportRepository;
import com.talex.server.services.report.ReportService;
import com.talex.server.utils.PageUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ModerationTicketRepository ticketRepository;
    private final IReportMapper reportMapper;
    private final Sender questDBSender;

    @Override
    @Transactional
    public ReportResponseDto createReport(String currentUserId, String role, ReportRequestDto requestDto) {
        // Kiểm tra xem user đã báo cáo nội dung này chưa (chống spam)
        validateAlreadyReport(currentUserId, requestDto);

        // Tìm hoặc tạo mới ModerationTicket (Gộp các báo cáo cùng nội dung)
        ModerationTicket ticket = ticketRepository.findByTargetTypeAndTargetIdAndStatusIn(
                        requestDto.getTargetType(),
                        requestDto.getTargetId(),
                        List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)
                )
                .orElseGet(() -> ModerationTicket.builder()
                        .targetType(requestDto.getTargetType())
                        .targetId(requestDto.getTargetId())
                        .reportCount(0)
                        .priorityScore(0)
                        .status(TicketStatus.OPEN)
                        .build());

        // Cập nhật điểm ưu tiên & số lượng báo cáo cho Ticket
        ticket.setReportCount(ticket.getReportCount() + 1);
        ticket.setPriorityScore(ticket.getPriorityScore() + calculateReasonWeight(requestDto.getReason()));
        ticket = ticketRepository.save(ticket);

        // 3. Tạo Entity Report
        Report report = reportMapper.toEntity(requestDto);
        report.setReporterId(currentUserId);
        report.setStatus(ReportStatus.PENDING);
        report.setModerationTicket(ticket);
        Report savedReport = reportRepository.save(report);

        // 4. Ghi Audit Log
        questDBSender.table("report_logs")
                .symbol("ticket_id", ticket.getTicketId())
                .symbol("actor_id", currentUserId)
                .symbol("report_role", role)
                .symbol("action_type", AuditActionType.REPORT_SUBMITTED.toString())
                .symbol("target_type", requestDto.getTargetType().toString())
                .symbol("target_id", requestDto.getTargetId())
                .symbol("payload", "Submitted report: " + requestDto.getReason())
                .at(Instant.now());

        return reportMapper.toResponseDto(savedReport);
    }

    /// Kiểm tra xem user đã báo cáo nội dung này chưa (chống spam)
    private void validateAlreadyReport(String currentUserId, ReportRequestDto requestDto) {
        boolean exists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                currentUserId, requestDto.getTargetType(), requestDto.getTargetId(), ReportStatus.PENDING);
        if (exists) throw new ModerationException(ModerationErrorCode.DUPLICATE_REPORT);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<ReportResponseDto> getUserReports(String currentUserId, BaseFilterRequestDto filterRequest) {
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Report> pageResult = reportRepository.findByReporterId(currentUserId, pageable);
        List<ReportResponseDto> content = pageResult.stream().map(reportMapper::toResponseDto).toList();

        return BasePageResponse.<ReportResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    private int calculateReasonWeight(ReportReason reason) {
        return switch (reason) {
            case COPYRIGHT -> 5;
            case ADULT_CONTENT -> 4;
            case BAD_LANGUAGE -> 2;
            case SPAM -> 1;
            default -> 1;
        };
    }
}