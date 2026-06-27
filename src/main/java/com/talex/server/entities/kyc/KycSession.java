package com.talex.server.entities.kyc;

import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.enums.kyc.KycStatus;
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
@Table(name = "kyc_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kyc_session_id")
    private String kycSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KycStatus status;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @OneToMany(mappedBy = "kycSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KycStep> kycSteps = new ArrayList<>();

    @OneToOne(mappedBy = "kycSession")
    private CreatorIdentity creatorIdentity;
}
