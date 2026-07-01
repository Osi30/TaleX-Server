package com.talex.server.dtos.requests;

import com.talex.server.enums.series.TagStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagRequestDto {
    @NotBlank
    private String tagName;

    private String description;

    private String slug;

    private TagStatus status;

    private String actorId;
}
