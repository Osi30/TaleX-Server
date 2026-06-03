package com.talex.server.dtos.requests;

import com.talex.server.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    @NotBlank
    private String categoryName;

    private String description;

    private String slug;

    private CategoryStatus status;

    private String actorId;
}
