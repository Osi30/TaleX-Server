package com.talex.server.entities.config;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "base_premium_share", nullable = false)
    private Double basePremiumShare;

    @Column(name = "base_unlock_share", nullable = false)
    private Double baseUnlockShare;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}