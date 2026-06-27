package com.talex.server.dtos.requests;

import com.talex.server.enums.SeasonStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRequestDto {
    private Integer seasonNumber;

    @NotBlank
    private String title;

    private String description;

    private SeasonStatus status;

}
