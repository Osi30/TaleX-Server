package com.talex.server.entities.ads;

import com.talex.server.entities.auth.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "advertise_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertiseProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private UUID profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "wallet_balance", nullable = false)
    @Builder.Default
    private Long walletBalance = 0L;

    @Column(name = "billing_info", columnDefinition = "TEXT")
    private String billingInfo;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    private String website;

    @Column(name = "is_setup_completed", nullable = false)
    @Builder.Default
    private Boolean isSetupCompleted = false;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdCampaign> campaigns = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
