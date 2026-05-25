package com.talex.server.dtos.responses.liveness;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FptAiLivenessResponse {
    private String code;
    private String message;
    private LivenessData liveness;

    @JsonProperty("face_match")
    private FaceMatchData faceMatch;
}
