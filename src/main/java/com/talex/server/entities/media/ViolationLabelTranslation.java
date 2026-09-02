package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "violation_label_translations",
        indexes = @Index(name = "idx_vlt_aws_label", columnList = "aws_label", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationLabelTranslation extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "translation_id", updatable = false, nullable = false)
    private UUID translationId;

    @Column(name = "aws_label", nullable = false, unique = true)
    private String awsLabel;

    @Column(name = "vietnamese_text", nullable = false)
    private String vietnameseText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ViolationLabelCategory category;
}
