package com.talex.server.dtos.interaction.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @NotNull
    @JsonProperty("current_position")
    private Double currentPosition;

    // Số giây thực tế
    @JsonProperty("heartbeat_value")
    @NotNull
    @DecimalMin(value = "1.0", message = "Giá trị phải lớn hơn hoặc bằng 1.0")
    @DecimalMax(value = "5.0", message = "Giá trị phải nhỏ hơn hoặc bằng 5.0")
    private Double heartbeatValue;

    @NotNull
    private String event;

    @JsonIgnore
    private LocalDateTime timestamp = LocalDateTime.now();

    @JsonIgnore
    private String ipAddress;
}
