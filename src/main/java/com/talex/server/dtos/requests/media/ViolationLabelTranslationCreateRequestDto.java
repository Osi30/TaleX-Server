package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLabelTranslationCreateRequestDto {

    @NotBlank
    private String awsLabel;

    @NotBlank
    private String vietnameseText;

    private String category;
}
