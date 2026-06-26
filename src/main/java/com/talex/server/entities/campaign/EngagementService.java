package com.talex.server.entities.campaign;

import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.enums.engagement.EngagementType;
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
@Table(name = "engagement_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngagementService {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "engagement_service_id")
    private String engagementServiceId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "engagement_type", nullable = false)
    private EngagementType engagementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "engagement_target", nullable = false)
    private EngagementTarget engagementTarget;

    @Column(name = "price")
    private Long price;

    @Column(name = "target_value")
    private Long targetValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "engagementService", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Campaign> campaigns = new ArrayList<>();
}
