package com.talex.server.services;

import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;

import java.util.UUID;

public interface IInteractionService {
    void processInteraction(UUID accountId, InteractionRequest request);
    void processTelemetry(UUID accountId, WatchTimeRequest request);
}
