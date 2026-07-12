package com.talex.server.dtos.interaction.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ViewRequest {
    @NotBlank(message = "Phiên xem không được để trống")
    private String sessionId;

    @NotBlank(message = "Phim không được để trống")
    private String episodeId;

    @JsonIgnore
    private UUID accountId;

    @JsonIgnore
    private String ipAddress;
}
