package com.talex.server.services.ekyc;

import com.talex.server.dtos.requests.filters.KycSessionFilterRequestDto;
import com.talex.server.dtos.requests.kyc.KycSessionRequestDto;
import com.talex.server.dtos.responses.kyc.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.kyc.KycSessionResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.kyc.KycSession;

public interface IKycSessionService {
    String createSession(Creator creator);

    KycSessionResponseDto getSessionById(String kycSessionId);

    KycSessionPageResponseDto filterAndSortSessions(KycSessionFilterRequestDto filterRequest);

    KycSessionResponseDto updateSession(String kycSessionId, KycSessionRequestDto requestDto);

    KycSession getById(String kycSessionId);

    KycSession getInProgressSession(String kycSessionId);
}
