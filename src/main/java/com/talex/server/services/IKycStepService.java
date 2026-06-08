package com.talex.server.services;

import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.entities.KycSession;
import com.talex.server.entities.KycStep;
import com.talex.server.enums.StepType;
import com.talex.server.records.EKycResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IKycStepService {

    KycStep createKycStep(StepType stepType, KycSession session);

    KycStepResponseDto getKycStepById(String kycStepId);

    EKycResult scanID(MultipartFile image, StepType stepType, String sessionId);

    EKycResult processLiveness(MultipartFile video, MultipartFile cmnd, String sessionId);

    List<KycStepResponseDto> filterKycSteps(Map<String, Object> params);
}
