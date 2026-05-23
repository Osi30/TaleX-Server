package com.talex.server.entities;

import com.talex.server.enums.StepType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_step")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kyc_step_id")
    private String kycStepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 50)
    private StepType stepType;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 100)
    private String provider;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private KycSession kycSession;
}