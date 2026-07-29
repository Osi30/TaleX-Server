package com.talex.server.entities.ads;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lưu cấu hình hệ thống quảng cáo dưới dạng key-value.
 * Ví dụ: key="POPUP_ALLOWED_ROUTES", value="/,/series,/comics,/watch"
 */
@Entity
@Table(name = "ad_system_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id")
    private UUID configId;

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "description")
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
