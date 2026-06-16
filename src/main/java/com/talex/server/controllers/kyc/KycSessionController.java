package com.talex.server.controllers.kyc;

import com.talex.server.annotations.ValidFile;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.kyc.KycSessionRequestDto;
import com.talex.server.dtos.requests.filters.KycSessionFilterRequestDto;
import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.enums.kyc.StepType;
import com.talex.server.policies.FilePolicy;
import com.talex.server.records.EKycResult;
import com.talex.server.services.ekyc.IKycSessionService;
import com.talex.server.services.ekyc.IKycStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/kyc-sessions")
@RequiredArgsConstructor
@Validated
public class KycSessionController {
    private final IKycSessionService kycSessionService;
    private final IKycStepService kycStepService;

    @GetMapping("/{kycSessionId}")
    public ResponseEntity<BaseResponse> getSessionById(
            @PathVariable String kycSessionId
    ) {
        KycSessionResponseDto responseDto = kycSessionService.getSessionById(kycSessionId);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy thông tin phiên KYC thành công!")
                        .data(responseDto)
                        .build());
    }

    @GetMapping
    public ResponseEntity<BaseResponse> filterAndSortSessions(
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        KycSessionPageResponseDto responsePage = kycSessionService
                .filterAndSortSessions(KycSessionFilterRequestDto.builder()
                        .criteria(criteria)
                        .statuses(statuses)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build());

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy danh sách phiên KYC thành công!")
                        .data(responsePage)
                        .build());
    }

    @PutMapping("/{kycSessionId}")
    public ResponseEntity<BaseResponse> updateSession(
            @PathVariable String kycSessionId,
            @RequestBody KycSessionRequestDto requestDto
    ) {
        KycSessionResponseDto responseDto = kycSessionService.updateSession(kycSessionId, requestDto);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Cập nhật phiên KYC thành công!")
                        .data(responseDto)
                        .build());
    }

    @PostMapping(
            value = "/{kycSessionId}/id-card/front-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BaseResponse> scanFrontIdCard(
            @PathVariable
            String kycSessionId,
            @RequestParam("frontImage") @ValidFile(policy = FilePolicy.KYC_IMAGE)
            MultipartFile frontImage
    ) {
        EKycResult response = kycStepService
                .scanID(frontImage, StepType.FRONT_ID, kycSessionId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Nhận diện trích xuất CCCD mặt trước thành công!")
                        .data(response)
                        .build());
    }

    @PostMapping(
            value = "/{kycSessionId}/id-card/back-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BaseResponse> scanBackIdCard(
            @PathVariable
            String kycSessionId,
            @RequestParam("backImage") @ValidFile(policy = FilePolicy.KYC_IMAGE)
            MultipartFile backImage
    ) {
        EKycResult response = kycStepService
                .scanID(backImage, StepType.BACK_ID, kycSessionId);

        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Nhận diện trích xuất CCCD mặt sau thành công!")
                        .data(response)
                        .build()
        );
    }

    @PostMapping(
            value = "/{kycSessionId}/liveness",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BaseResponse> verifyLiveness(
            @PathVariable
            String kycSessionId,
            @RequestParam("video") @ValidFile(policy = FilePolicy.KYC_VIDEO)
            MultipartFile video,
            @RequestParam("cmnd") @ValidFile(policy = FilePolicy.KYC_IMAGE)
            MultipartFile image
    ) {
        EKycResult stepResult = kycStepService
                .processLiveness(video, image, kycSessionId);

        return ResponseEntity.ok(
                new BaseResponse(
                        200,
                        "Kiểm tra thực thể sống hoàn tất!",
                        stepResult)
        );
    }
}
