package com.talex.server.dtos.requests;

import com.talex.server.enums.series.EpisodeStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboEpisodeRequestDto {
    @NotBlank(message = "Title is required")
    @Size(max = 250, message = "Title must not exceed 250 characters")
    private String title;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be at least 0")
    private Long priceVnd;
    
    private EpisodeStatus status;

    private List<String> episodeIds;
}
