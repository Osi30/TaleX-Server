package com.talex.server.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.requests.KycStepRequestDto;
import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.entities.KycSession;
import com.talex.server.entities.KycStep;
import com.talex.server.enums.StepType;
import com.talex.server.mappers.IKycStepMapper;
import com.talex.server.records.OcrResult;
import com.talex.server.repositories.KycStepRepository;
import com.talex.server.services.IEKycService;
import com.talex.server.services.IKycSessionService;
import com.talex.server.services.IKycStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.talex.server.specifications.KycStepSpec;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class KycStepService implements IKycStepService {
    @Value("${eKYC.id-recognition.provider}")
    private String provider;

    private final KycStepRepository kycStepRepository;
    private final IKycSessionService kycSessionService;
    private final IEKycService eKycService;
    private final IKycStepMapper kycStepMapper;
    private final ObjectMapper objectMapper;


    @Override
    public KycStep createKycStep(StepType stepType, KycSession kycSession) {
        KycStep kycStep = kycStepMapper.toDefaultEntity(KycStepRequestDto.builder()
                .stepType(stepType)
                .provider(provider)
                .build());
        kycStep.setKycSession(kycSession);
        return kycStepRepository.save(kycStep);
    }

    @Override
    @Transactional(readOnly = true)
    public KycStepResponseDto getKycStepById(String kycStepId) {
        KycStep kycStep = kycStepRepository.findById(kycStepId)
                .orElseThrow(() -> new RuntimeException("KycStep not found with id: " + kycStepId));

        return kycStepMapper.toResponseDto(kycStep);
    }

    @Override
    public KycStepResponseDto scanID(MultipartFile image, StepType stepType, String sessionId) {
        validateStepType(stepType);

        // Khởi tạo Step
        KycSession kycSession = kycSessionService.getById(sessionId);
        KycStep scanImage = createKycStep(stepType, kycSession);

        try {
            // Gọi dịch vụ OCR
            OcrResult ocrResult = callOcrProviderApi(stepType, image);

            // Cập nhật dữ liệu (success)
            applyOcrSuccessState(scanImage, ocrResult);

        } catch (Exception exception) {
            // ập nhật dữ liệu (fail)
            applyOcrFailureState(scanImage, exception);
            throw exception;

        } finally {
            // Ghi vết trạng thái cuối cùng
            persistKycStepResult(scanImage);
        }

        return kycStepMapper.toResponseDto(scanImage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycStepResponseDto> filterKycSteps(Map<String, Object> params) {
        Specification<KycStep> spec = KycStepSpec.filterByCriteria(params);
        List<KycStep> steps = kycStepRepository.findAll(spec);
        return steps.stream().map(kycStepMapper::toResponseDto).toList();
    }

    private void validateStepType(StepType stepType) {
        if (stepType != StepType.FRONT_ID && stepType != StepType.BACK_ID) {
            throw new IllegalArgumentException("Loại bước KYC chưa được hỗ trợ hoặc không hợp lệ.");
        }
    }

    private OcrResult callOcrProviderApi(StepType stepType, MultipartFile image) {
        return switch (stepType) {
            case FRONT_ID -> executeFrontIdOcr(image);
            case BACK_ID -> executeBackIdOcr(image);
            default -> throw new IllegalStateException("Chưa cấu hình xử lý cho bước: " + stepType);
        };
    }

    private OcrResult executeFrontIdOcr(MultipartFile image) {
        FptAiIdFrontResponse response = eKycService.processFrontSide(image);
        boolean isSuccess = (response.getErrorCode() == 0);
        String message = isSuccess ? "Nhận diện mặt trước CCCD thành công!" : response.getErrorMessage();
        return new OcrResult(isSuccess, message, objectMapper.valueToTree(response));
    }

    private OcrResult executeBackIdOcr(MultipartFile image) {
        FptAiIdBackResponse response = eKycService.processBackSide(image);
        boolean isSuccess = (response.getErrorCode() == 0);
        String message = isSuccess ? "Nhận diện mặt sau CCCD thành công!" : response.getErrorMessage();
        return new OcrResult(isSuccess, message, objectMapper.valueToTree(response));
    }

    private void applyOcrSuccessState(KycStep kycStep, OcrResult ocrResult) {
        kycStep.setIsSuccess(ocrResult.isSuccess());
        kycStep.setMessage(ocrResult.message());
        kycStep.setRawResponse(ocrResult.rawResponse());
    }

    private void applyOcrFailureState(KycStep kycStep, Exception exception) {
        kycStep.setIsSuccess(false);
        kycStep.setMessage("Thất bại: " + exception.getMessage());
    }

    private void persistKycStepResult(KycStep kycStep) {
        kycStepRepository.updateKycResult(
                kycStep.getKycStepId(),
                kycStep.getIsSuccess(),
                kycStep.getMessage(),
                kycStep.getRawResponse()
        );
    }
}
