package com.talex.server.controllers.kyc;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.services.ekyc.IKycStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kyc-steps")
@RequiredArgsConstructor
public class KycStepController {
    private final IKycStepService kycStepService;

    @GetMapping("/{kycStepId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getKycStepById(
            @PathVariable String kycStepId) {
        KycStepResponseDto response = kycStepService.getKycStepById(kycStepId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.builder()
                        .code(200)
                        .message("Lấy thông tin bước KYC thành công!")
                        .data(response)
                        .build());
    }


    @GetMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> filterKycSteps(
            @RequestParam Map<String, Object> params
    ) {
        List<KycStepResponseDto> response = kycStepService.filterKycSteps(params);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseResponse.builder()
                        .code(200)
                        .message("Lọc các bước KYC thành công!")
                        .data(response)
                        .build());
    }
}
