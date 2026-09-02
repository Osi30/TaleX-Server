package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Table(name = "violation_label_categories",
        indexes = @Index(name = "idx_vlc_name", columnList = "name", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationLabelCategory extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", updatable = false, nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}
