package com.talex.server.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_identity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_identity_id")
    private String creatorIdentityId;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "full_name", length = 150)
    private String fullName;

    private LocalDate dob;

    @Column(length = 20)
    private String sex;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Date of expiry
    private LocalDate doe;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false, unique = true)
    private Creator creator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kyc_session_id", unique = true)
    private KycSession kycSession;
}
