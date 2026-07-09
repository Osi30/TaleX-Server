package com.talex.server.entities.interaction;

import com.talex.server.entities.Account;
import com.talex.server.entities.series.Episode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "account_likes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"account_id", "episode_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_like_id")
    private String accountLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}