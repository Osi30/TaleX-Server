package com.talex.server.entities.ads;

import com.talex.server.enums.ads.AdMediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ad_creatives")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCreative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creative_id")
    private UUID creativeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private AdMediaType mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "target_url", nullable = false, columnDefinition = "TEXT")
    private String targetUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
