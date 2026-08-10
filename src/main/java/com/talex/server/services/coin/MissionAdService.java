package com.talex.server.services.coin;

import com.talex.server.dtos.responses.coin.AdSessionResponseDto;

import java.util.UUID;

public interface MissionAdService {

    AdSessionResponseDto startAdSession(UUID accountId, String missionCode);

    void completeAdSession(UUID accountId, String sessionId);
}
