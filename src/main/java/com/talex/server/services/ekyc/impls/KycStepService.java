package com.talex.server.services.ekyc.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.requests.kyc.KycStepRequestDto;
import com.talex.server.dtos.responses.kyc.KycStepResponseDto;
import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.dtos.responses.idrecognition.front.FrontData;
import com.talex.server.dtos.responses.liveness.FptAiLivenessResponse;
import com.talex.server.entities.Account;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.entities.kyc.KycSession;
import com.talex.server.entities.kyc.KycStep;
import com.talex.server.enums.kyc.KycStatus;
import com.talex.server.enums.kyc.StepType;
import com.talex.server.exceptions.codes.KycStepErrorCode;
import com.talex.server.exceptions.details.KycStepException;
import com.talex.server.mappers.kyc.IKycStepMapper;
import com.talex.server.records.EKycResult;
import com.talex.server.repositories.*;
import com.talex.server.repositories.creator.CreatorIdentityRepository;
import com.talex.server.repositories.kyc.KycSessionRepository;
import com.talex.server.repositories.kyc.KycStepRepository;
import com.talex.server.services.ekyc.IEKycService;
import com.talex.server.services.ekyc.IKycSessionService;
import com.talex.server.services.ekyc.IKycStepService;
import com.talex.server.services.IRoleService;
import com.talex.server.specifications.KycStepSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KycStepService implements IKycStepService {
    @Value("${eKYC.id-recognition.provider}")
    private String provider;

    private final KycStepRepository kycStepRepository;
    private final KycSessionRepository kycSessionRepository;
    private final CreatorIdentityRepository creatorIdentityRepository;
    private final AccountRepository accountRepository;
    private final IRoleService roleService;
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
    public EKycResult scanID(MultipartFile image, StepType stepType, String sessionId) {
        validateStepType(stepType);

        // Khởi tạo Step
        KycSession kycSession = kycSessionService.getInProgressSession(sessionId);
        KycStep scanImage = createKycStep(stepType, kycSession);
        CreatorIdentity identity = kycSession.getCreator().getCreatorIdentity();

        EKycResult eKycResult;
        try {
            // Gọi dịch vụ OCR
            eKycResult = callOcrProviderApi(stepType, image, identity);

            // Cập nhật dữ liệu (success)
            applySuccessState(scanImage, eKycResult);

        } catch (Exception exception) {
            // Cập nhật dữ liệu (fail)
            applyFailureState(scanImage, exception);
            throw new KycStepException(KycStepErrorCode.KYC_STEP_PROCESSING_FAILED,
                    "Xử lý scan ID thất bại: " + exception.getMessage(), exception);

        } finally {
            // Ghi vết trạng thái cuối cùng
            persistKycStepResult(scanImage);
        }

        return new EKycResult(eKycResult.isSuccess(), eKycResult.message(), null);
    }

    @Override
    public EKycResult processLiveness(MultipartFile video, MultipartFile cmnd, String sessionId) {
        // Khởi tạo Step
        KycSession kycSession = kycSessionService.getInProgressSession(sessionId);
        CreatorIdentity identity = kycSession.getCreator().getCreatorIdentity();

        // Kiểm tra trùng cccd đã đăng kí
        if (creatorIdentityRepository
                .existsByIdNumberAndKycSessionIsNotNull(identity.getIdNumber())){
            throw new KycStepException(KycStepErrorCode.KYC_STEP_ID_NUMBER_ALREADY_EXIST);
        }

        KycStep kycStep = createKycStep(StepType.LIVENESS_FACEMATCH, kycSession);
        EKycResult eKycResult;

        try {
            // Gọi dịch vụ Liveness - Facematch
            eKycResult = executeLiveness(video, cmnd);

            // Cập nhật dữ liệu (success)
            applySuccessState(kycStep, eKycResult);
            if (eKycResult.isSuccess()) {
                kycSession.setCompletedAt(LocalDateTime.now());
                kycSession.setStatus(KycStatus.SUCCESS);
                kycSessionRepository.save(kycSession);

                identity.setKycSession(kycSession);
                creatorIdentityRepository.save(identity);

                Account account = kycSession.getCreator().getAccount();
                account.setRole(roleService.findByCode("CREATOR"));
                accountRepository.save(account);
            }

        } catch (Exception ex) {
            // Cập nhật dữ liệu (fail)
            applyFailureState(kycStep, ex);
            throw new KycStepException(KycStepErrorCode.KYC_STEP_PROCESSING_FAILED,
                    "Xử lý Liveness thất bại: " + ex.getMessage(), ex);
        } finally {
            persistKycStepResult(kycStep);
        }

        return new EKycResult(eKycResult.isSuccess(), eKycResult.message(), null);
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

    private EKycResult callOcrProviderApi(StepType stepType, MultipartFile image, CreatorIdentity identity) {
        return switch (stepType) {
            case FRONT_ID -> executeFrontIdOcr(image, identity);
            case BACK_ID -> executeBackIdOcr(image);
            default -> throw new KycStepException(KycStepErrorCode.KYC_STEP_NOT_SUPPORTED,
                    "Chưa cấu hình xử lý cho bước KYC: " + stepType);
        };
    }

    private EKycResult executeFrontIdOcr(MultipartFile image, CreatorIdentity identity) {
        FptAiIdFrontResponse response = eKycService.processFrontSide(image);
        boolean isSuccess = (response.getErrorCode() == 0);
        String message = isSuccess ? "Nhận diện mặt trước CCCD thành công!" : response.getErrorMessage();

        // Update Creator Identity Info
        FrontData data = response.getData().getFirst();
        
        // Không trùng cccd đã đăng kí
        if (!creatorIdentityRepository.existsByIdNumberAndKycSessionIsNotNull(data.getId())){
            identity.setAddress(data.getAddress());
            identity.setSex(data.getSex());
            identity.setIdNumber(data.getId());
            identity.setFullName(data.getName());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            identity.setDob(LocalDate.parse(data.getDob(), formatter));
            identity.setDoe(LocalDate.parse(data.getDoe(), formatter));
            creatorIdentityRepository.save(identity);
            return new EKycResult(isSuccess, message, objectMapper.valueToTree(response));
        }

        return new EKycResult(!isSuccess, "Trùng cccd với tài khoản đã đăng ký", objectMapper.valueToTree(response));
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
