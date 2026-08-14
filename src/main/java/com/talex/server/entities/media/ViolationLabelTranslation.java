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

    // Nhãn gốc AWS Rekognition (VD "Explicit Nudity") — key tra cứu thật, KHÔNG được sửa
    // sau khi tạo (xem ViolationLabelTranslationServiceImpl.update()) vì đây chính là chuỗi
    // Rekognition trả về lúc kiểm duyệt thật, sửa sai sẽ làm nhãn đó "mất tích" khỏi tra cứu.
    @Column(name = "aws_label", nullable = false, unique = true)
    private String awsLabel;

    @Column(name = "vietnamese_text", nullable = false)
    private String vietnameseText;

    // Nhóm L1 taxonomy Rekognition (VD "Violence") — chỉ dùng để nhóm hiển thị trong Admin
    // CRUD, KHÔNG dùng cho tra cứu FE (tra cứu luôn phẳng theo awsLabel).
    @Column(name = "category")
    private String category;
}
