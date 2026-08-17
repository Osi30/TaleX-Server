package com.talex.server.entities.series;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// CRUD-able thay thế cho enum ContentWarningGroup cứng trước đây — Creator tự khai nhóm
// nội dung nhạy cảm (VD "Bạo lực / Máu me") cho series khi ageRating=MATURE. Series.contentWarnings
// tham chiếu tới đây qua "code" (String), không phải qua khóa ngoại UUID — vì contentWarnings
// đang lưu dạng @ElementCollection<String> vào bảng series_content_warnings riêng, không
// join trực tiếp bảng này.
@Entity
@Table(name = "content_warning_categories",
        indexes = @Index(name = "idx_cwc_code", columnList = "code", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentWarningCategory extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", updatable = false, nullable = false)
    private UUID categoryId;

    // Mã định danh ổn định (VD "SEXUAL_NUDITY") — Series.contentWarnings và bảng map AWS
    // label -> nhóm trong ContentPipelineServiceImpl đều tham chiếu bằng đúng chuỗi này.
    // KHÔNG được sửa sau khi tạo (xem ContentWarningCategoryServiceImpl.update()) — đổi code
    // sẽ làm "đứt" tham chiếu từ các series đã khai báo trước đó và khỏi bảng map AI.
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    // Ẩn khỏi form khai báo mới (list() public chỉ trả active=true) mà không xóa hẳn — giữ
    // nguyên các series đã khai báo nhóm này trước đó không bị mất dữ liệu.
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
