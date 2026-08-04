package com.talex.server.repositories.report;

import com.talex.server.entities.report.Report;
import com.talex.server.enums.report.ReportStatus;
import com.talex.server.enums.report.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository
        extends JpaRepository<Report, String>, JpaSpecificationExecutor<Report> {

    // Kiểm tra xem User này đã báo cáo nội dung này và đang chờ xử lý hay chưa (chống spam)
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            String reporterId, TargetType targetType, String targetId, ReportStatus status);

    // Lấy tất cả báo cáo chi tiết thuộc về 1 Ticket
    List<Report> findByModerationTicket_TicketId(String ticketId);

    // Lấy danh sách báo cáo cá nhân do User đó đã gửi
    Page<Report> findByReporterId(String reporterId, Pageable pageable);
}