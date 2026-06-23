package com.talex.server.dtos.requests.interaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.talex.server.enums.InteractionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionRequest {
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("episode_id")
    private String episodeId;

    private InteractionType interactionType;

    @JsonIgnore
    private LocalDateTime timestamp = LocalDateTime.now();
}
