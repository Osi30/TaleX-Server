package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.WatchTimeRequest;

import java.util.UUID;

public interface IWatchSessionService {
    void sendWatchHeartbeat(WatchTimeRequest request, UUID accountId, String ipAddress);;
}
