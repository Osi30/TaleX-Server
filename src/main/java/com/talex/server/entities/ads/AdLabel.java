package com.talex.server.entities.ads;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_labels", indexes = {
        @Index(name = "idx_ad_label_profile_id", columnList = "profile_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "label_id")
    private UUID labelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private AdvertiseProfile profile;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color", nullable = false)
    private String color;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
