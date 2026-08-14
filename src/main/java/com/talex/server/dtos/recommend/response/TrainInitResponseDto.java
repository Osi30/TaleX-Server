package com.talex.server.dtos.recommend.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainInitResponseDto {
    private String status;
    private String message;

    @JsonProperty("total_samples_generated")
    private Integer totalSamplesGenerated;

    @JsonProperty("model_saved_at")
    private String modelSavedAt;
}