package com.talex.server.entities.creator;

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
@Table(name = "creator_wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id")
    private String walletId;

    @Column(name = "current_balance", nullable = false)
    @Builder.Default
    private Long currentBalance = 0L;

    @Column(name = "total_balance", nullable = false)
    @Builder.Default
    private Long totalBalance = 0L;

    @Column(name = "estimated_net_balance", nullable = false)
    @Builder.Default
    private Long estimatedNetBalance = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false, unique = true)
    private Creator creator;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CreatorWalletChange> walletChanges = new ArrayList<>();
}
