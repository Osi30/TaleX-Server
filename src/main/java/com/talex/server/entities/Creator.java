package com.talex.server.entities;

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
@Table(name = "creator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @OneToOne(mappedBy = "creator")
    private CreatorIdentity creatorIdentity;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentProfile> paymentProfiles = new ArrayList<>();
}
