package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.InteractionRequest;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;

import java.util.UUID;

public interface IInteractionService {
    void processTelemetry(UUID accountId, WatchTimeRequest request);
}
