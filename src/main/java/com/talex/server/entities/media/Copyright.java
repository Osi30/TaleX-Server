package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "copyright",
        indexes = {
                @Index(name = "idx_copyright_code", columnList = "code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Copyright extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "copyright_id")
    private String copyrightId;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "legal_url", columnDefinition = "TEXT")
    private String legalUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // JSON string: {"canShare": true, "canModify": false, "canCommercialUse": false}
    @Column(columnDefinition = "TEXT")
    private String permissions;
}
