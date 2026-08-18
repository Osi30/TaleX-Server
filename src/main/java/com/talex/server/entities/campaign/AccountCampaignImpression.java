package com.talex.server.entities.campaign;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "account_campaign_impressions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"account_id", "campaign_series_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCampaignImpression implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_series_id", nullable = false)
    private CampaignSeries campaignSeries;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}