package com.talex.server.dtos.responses.liveness;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LivenessData {
    private String code;
    private String message;

    @JsonProperty("is_live")
    private String isLive;

    @JsonProperty("spoof_prob")
    private String spoofProb;

    @JsonProperty("need_to_review")
    private String needToReview;

    @JsonProperty("is_deepfake")
    private String isDeepfake;

    @JsonProperty("deepfake_prob")
    private String deepfakeProb;

    private String warning;
}
