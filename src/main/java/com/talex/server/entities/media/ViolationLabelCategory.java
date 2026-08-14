package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// Nhóm hiển thị cho ViolationLabelTranslation (VD "Violence", "Explicit") — tách thành
// entity riêng CRUD được, thay vì chuỗi tự do trước đây (Admin gõ tay không kiểm soát,
// dễ tạo tên trùng ý nghĩa khác chính tả như "Violence"/"violence"/"Bạo lực").
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
