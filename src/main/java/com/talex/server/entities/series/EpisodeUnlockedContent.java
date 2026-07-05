package com.talex.server.entities.series;

import com.talex.server.entities.Account;
import com.talex.server.entities.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "episode_unlocked_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpisodeUnlockedContent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "purchase_price_vnd")
    private Long purchasePriceVnd;

    @Column(name = "unlock_method")
    @Builder.Default
    private String unlockMethod = "ORDER";
}
