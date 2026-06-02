package com.talex.server.services;

import com.talex.server.dtos.requests.filters.KycSessionFilterRequestDto;
import com.talex.server.dtos.requests.KycSessionRequestDto;
import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.entities.KycSession;

public interface IKycSessionService {
    String createSession(Creator creator);

    KycSessionResponseDto getSessionById(String kycSessionId);

    KycSessionPageResponseDto filterAndSortSessions(KycSessionFilterRequestDto filterRequest);

    KycSessionResponseDto updateSession(String kycSessionId, KycSessionRequestDto requestDto);

    KycSession getById(String kycSessionId);
}
