package com.talex.server.dtos.requests.ads;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdLabelRequestDto {
    @NotBlank(message = "Label name is required")
    private String name;
    
    @NotBlank(message = "Label color is required")
    private String color;
}
