package com.talex.server.dtos.requests.interaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WatchTimeRequest {
    @NotNull
    @JsonProperty("session_id")
    private String sessionId;

    @NotNull
    @JsonProperty("episode_id")
    private String episodeId;

    // Vị trí hiện tại
    @JsonProperty("current_position")
    private Double currentPosition;

    // Số giây thực tế vừa xem
    @JsonProperty("duration")
    private Long duration;

    @JsonIgnore
    private LocalDateTime timestamp = LocalDateTime.now();
}
