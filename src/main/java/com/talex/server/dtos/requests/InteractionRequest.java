package com.talex.server.dtos.requests;

import com.talex.server.enums.ContentType;
import com.talex.server.enums.InteractionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionRequest {
    private String episodeId;
    private ContentType contentType;
    private InteractionType interactionType;
    private LocalDateTime timestamp = LocalDateTime.now();
}
