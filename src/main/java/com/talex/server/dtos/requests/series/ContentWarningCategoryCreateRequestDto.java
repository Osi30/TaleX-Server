package com.talex.server.dtos.requests.series;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentWarningCategoryCreateRequestDto {

    // UPPER_SNAKE_CASE — khớp quy ước enum cũ (VD "SEXUAL_NUDITY") để nhất quán với các
    // giá trị đã tồn tại trong dữ liệu series cũ.
    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "code phải viết hoa dạng UPPER_SNAKE_CASE, VD: SEXUAL_NUDITY")
    private String code;

    @NotBlank
    private String label;
}
