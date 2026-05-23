package com.talex.server.services;

import com.talex.server.dtos.requests.KycSessionFilterRequestDto;
import com.talex.server.dtos.requests.KycSessionRequestDto;
import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;

public interface IKycSessionService {
    KycSessionResponseDto createSession();

    KycSessionResponseDto getSessionById(String kycSessionId);

    KycSessionPageResponseDto filterAndSortSessions(KycSessionFilterRequestDto filterRequest);

    KycSessionResponseDto updateSession(String kycSessionId, KycSessionRequestDto requestDto);
}
