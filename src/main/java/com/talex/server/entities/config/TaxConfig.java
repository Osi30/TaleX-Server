package com.talex.server.entities.config;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "vat", nullable = false)
    private Double vat;

    @Column(name = "pit", nullable = false)
    private Double pit;

    @Column(name = "min_pit_amount")
    private Long minPitAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}