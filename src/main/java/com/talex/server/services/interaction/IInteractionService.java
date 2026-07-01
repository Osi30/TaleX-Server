package com.talex.server.services.interaction;

import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;

import java.util.UUID;

public interface IInteractionService {
    void processInteraction(UUID accountId, InteractionRequest request);
    void processTelemetry(UUID accountId, WatchTimeRequest request);
    void handleInteraction(UUID accountId, InteractionRequest request);
}
