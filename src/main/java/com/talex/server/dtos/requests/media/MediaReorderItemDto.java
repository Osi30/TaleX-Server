package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaReorderItemDto {
    @NotBlank
    private String mediaId;

    @NotNull
    private Integer displayOrder;
}
