package com.talex.server.entities.campaign;

import com.talex.server.entities.creator.Creator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaign_wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id")
    private String walletId;

    // Số dư khả dụng trong ví (Lưu tiền TRƯỚC THUẾ / Net Credit)
    @Column(name = "balance", nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Creator creator;

    @OneToMany(mappedBy = "campaignWallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CampaignWalletTransaction> transactions = new ArrayList<>();
}