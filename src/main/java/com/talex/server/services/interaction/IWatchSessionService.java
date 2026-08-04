package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.dtos.interaction.response.WatchSessionResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IWatchSessionService {
    void sendWatchHeartbeat(WatchTimeRequest request, UUID accountId, String ipAddress);
    Slice<WatchSessionResponseDto> getRecentWatchSessions(UUID accountId, Pageable pageable);
}
