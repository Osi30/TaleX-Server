package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLabelTranslationCreateRequestDto {

    @NotBlank
    private String awsLabel;

    @NotBlank
    private String vietnameseText;

    // Nullable — "Nhóm" là tùy chọn. Tham chiếu ViolationLabelCategory.categoryId (xem
    // GET /api/v1/violation-label-categories cho danh sách chọn).
    private UUID categoryId;
}
