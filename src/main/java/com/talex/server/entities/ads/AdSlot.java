package com.talex.server.entities.ads;

import com.talex.server.enums.ads.AdSlotType;
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
@Table(name = "ad_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "slot_id")
    private UUID slotId;

    @Column(name = "code_name", nullable = false, unique = true)
    private String codeName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AdSlotType type;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "total_view_of_price", nullable = false)
    private Long totalViewOfPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
