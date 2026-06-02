package com.talex.server.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.requests.KycStepRequestDto;
import com.talex.server.dtos.responses.liveness.FptAiLivenessResponse;
import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.entities.KycSession;
import com.talex.server.entities.KycStep;
import com.talex.server.enums.StepType;
import com.talex.server.exceptions.codes.KycStepErrorCode;
import com.talex.server.exceptions.details.KycStepException;
import com.talex.server.mappers.IKycStepMapper;
import com.talex.server.records.EKycResult;
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
                .orElseThrow(() -> new KycStepException(
                        KycStepErrorCode.KYC_STEP_NOT_FOUND,
                        "KycStep không tồn tại với id: " + kycStepId));

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
            EKycResult eKycResult = callOcrProviderApi(stepType, image);

            // Cập nhật dữ liệu (success)
            applySuccessState(scanImage, eKycResult);

        } catch (Exception exception) {
            // Cập nhật dữ liệu (fail)
            applyFailureState(scanImage, exception);
            if (exception instanceof KycStepException) {
                throw exception;
            }
            throw new KycStepException(KycStepErrorCode.KYC_STEP_PROCESSING_FAILED,
                    "Xử lý scan ID thất bại: " + exception.getMessage(), exception);

        } finally {
            // Ghi vết trạng thái cuối cùng
            persistKycStepResult(scanImage);
        }

        return kycStepMapper.toResponseDto(scanImage);
    }

    @Override
    public KycStepResponseDto processLiveness(MultipartFile video, MultipartFile cmnd, String sessionId) {
        // Khởi tạo Step
        KycSession kycSession = kycSessionService.getById(sessionId);
        KycStep kycStep = createKycStep(StepType.LIVENESS_FACEMATCH, kycSession);

        try {
            // Gọi dịch vụ Liveness - Facematch
            EKycResult eKycResult = executeLiveness(video, cmnd);

            // Cập nhật dữ liệu (success)
            applySuccessState(kycStep, eKycResult);

        } catch (Exception ex) {
            // Cập nhật dữ liệu (fail)
            applyFailureState(kycStep, ex);
            if (ex instanceof KycStepException) {
                throw ex;
            }
            throw new KycStepException(KycStepErrorCode.KYC_STEP_PROCESSING_FAILED,
                    "Xử lý Liveness thất bại: " + ex.getMessage(), ex);
        } finally {
            persistKycStepResult(kycStep);
        }

        return kycStepMapper.toResponseDto(kycStep);
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
            throw new KycStepException(KycStepErrorCode.INVALID_STEP_TYPE,
                    "Loại bước KYC chưa được hỗ trợ hoặc không hợp lệ: " + stepType);
        }
    }

    private EKycResult callOcrProviderApi(StepType stepType, MultipartFile image) {
        return switch (stepType) {
            case FRONT_ID -> executeFrontIdOcr(image);
            case BACK_ID -> executeBackIdOcr(image);
            default -> throw new KycStepException(KycStepErrorCode.KYC_STEP_NOT_SUPPORTED,
                    "Chưa cấu hình xử lý cho bước KYC: " + stepType);
        };
    }

    private EKycResult executeFrontIdOcr(MultipartFile image) {
        FptAiIdFrontResponse response = eKycService.processFrontSide(image);
        boolean isSuccess = (response.getErrorCode() == 0);
        String message = isSuccess ? "Nhận diện mặt trước CCCD thành công!" : response.getErrorMessage();
        return new EKycResult(isSuccess, message, objectMapper.valueToTree(response));
    }

    private EKycResult executeBackIdOcr(MultipartFile image) {
        FptAiIdBackResponse response = eKycService.processBackSide(image);
        boolean isSuccess = (response.getErrorCode() == 0);
        String message = isSuccess ? "Nhận diện mặt sau CCCD thành công!" : response.getErrorMessage();
        return new EKycResult(isSuccess, message, objectMapper.valueToTree(response));
    }

    private EKycResult executeLiveness(MultipartFile video, MultipartFile cmnd) {
        FptAiLivenessResponse response = eKycService.checkLiveness(video, cmnd);

        boolean hasLiveness = response != null && "200".equals(response.getCode()) && response.getLiveness() != null;
        boolean hasFaceMatch = response != null && response.getFaceMatch() != null;

        boolean isLive = hasLiveness && "true".equalsIgnoreCase(response.getLiveness().getIsLive());
        boolean isDeepfake = hasLiveness && "true".equalsIgnoreCase(response.getLiveness().getIsDeepfake());
        boolean isMatch = hasFaceMatch && "true".equalsIgnoreCase(response.getFaceMatch().getIsMatch());

        // Có đủ dữ liệu, là người thật, không phải deepfake, khuôn mặt trùng khớp
        boolean isSuccess = hasLiveness && hasFaceMatch && isLive && !isDeepfake && isMatch;

        // Phân tách thông báo lỗi chi tiết ra màn hình dựa trên kết quả thực tế
        String message = "Xác thực thành công!";
        if (!hasLiveness || !hasFaceMatch) {
            message = "Thất bại: Phản hồi hệ thống eKYC không đúng cấu trúc phân tích.";
        } else if (!isLive) {
            message = "Thất bại: Phát hiện thực thể sống không hợp lệ (Yêu cầu quay video trực tiếp).";
        } else if (isDeepfake) {
            message = "Thất bại: Hệ thống phát hiện dấu hiệu giả mạo khuôn mặt kĩ thuật số (Deepfake).";
        } else if (!isMatch) {
            message = "Thất bại: Khuôn mặt trong video không trùng khớp với ảnh chân dung trên CCCD (Độ khớp: " + response.getFaceMatch().getSimilarity() + "%).";
        }

        return new EKycResult(isSuccess, message, objectMapper.valueToTree(response));
    }

    private void applySuccessState(KycStep kycStep, EKycResult eKycResult) {
        kycStep.setIsSuccess(eKycResult.isSuccess());
        kycStep.setMessage(eKycResult.message());
        kycStep.setRawResponse(eKycResult.rawResponse());
    }

    private void applyFailureState(KycStep kycStep, Exception exception) {
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
