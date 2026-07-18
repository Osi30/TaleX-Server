package com.talex.server.entities.transaction;

import com.talex.server.enums.transaction.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id")
    private UUID invoiceId;

    // SePay eInvoice tạo hóa đơn bất đồng bộ (trả tracking_code trước, phải poll mới có
    // invoice_number/invoice_url thật) — nên các field này chỉ có giá trị sau khi COMPLETED.
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_series")
    private String invoiceSeries;

    @Column(name = "reservation_code")
    private String reservationCode;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "poll_attempts", nullable = false)
    @Builder.Default
    private Integer pollAttempts = 0;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // EAGER: InvoiceStatusPollingScheduler load Invoice ở 1 transaction rồi xử lý ở transaction
    // khác (Invoice detached lúc đó) — LAZY sẽ ném LazyInitializationException khi truy cập.
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
}