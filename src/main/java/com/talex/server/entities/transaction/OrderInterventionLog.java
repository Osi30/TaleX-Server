package com.talex.server.entities.transaction;

import com.talex.server.enums.transaction.OrderInterventionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for admin manual intervention on Orders (cancel / force-complete).
 * Written before the order is changed, never updated, never soft-deleted — same pattern as
 * MediaPurgeLog. Order is kept as a plain value (not an FK) for consistency with that pattern,
 * even though Order rows are never physically deleted here.
 */
@Entity
@Table(
        name = "order_intervention_log",
        indexes = {
                @Index(name = "idx_order_intervention_log_order", columnList = "order_id"),
                @Index(name = "idx_order_intervention_log_admin", columnList = "admin_account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInterventionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_intervention_log_id")
    private String orderInterventionLogId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "payment_code")
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private OrderInterventionAction action;

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status")
    private String newStatus;

    // Admin account that triggered the intervention.
    @Column(name = "admin_account_id", nullable = false)
    private String adminAccountId;

    // Mandatory human-provided justification, shown in the FE confirm modal.
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
