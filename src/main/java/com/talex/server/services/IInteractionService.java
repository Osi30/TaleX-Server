package com.talex.server.services;

import com.talex.server.dtos.requests.InteractionRequest;

import java.util.UUID;

public interface IInteractionService {
    void processInteraction(UUID accountId, InteractionRequest request);
}
