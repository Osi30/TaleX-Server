package com.talex.server.entities.kyc;

import com.fasterxml.jackson.databind.JsonNode;
import com.talex.server.converters.JsonNodeConverter;
import com.talex.server.entities.Account;
import com.talex.server.entities.Creator;
import com.talex.server.enums.kyc.KycActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private Account changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50, nullable = false)
    private KycActionType actionType;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "old_value", columnDefinition = "json")
    private JsonNode oldValue;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "new_value", columnDefinition = "json")
    private JsonNode newValue;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
