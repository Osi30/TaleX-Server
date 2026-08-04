package com.talex.server.entities.report;

import com.talex.server.enums.report.TargetType;
import com.talex.server.enums.report.TicketStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moderation_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private String ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "priority_score", nullable = false)
    @Builder.Default
    private Integer priorityScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assigned_staff_id")
    private String assignedStaffId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "moderationTicket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Report> reports = new ArrayList<>();
}