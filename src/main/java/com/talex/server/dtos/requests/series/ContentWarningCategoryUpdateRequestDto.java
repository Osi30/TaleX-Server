package com.talex.server.dtos.requests.series;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentWarningCategoryUpdateRequestDto {

    // "code" KHÔNG có ở đây — bất biến sau khi tạo, xem comment ở ContentWarningCategory.java.
    @NotBlank
    private String label;

    @NotNull
    private Boolean isActive;
}
