package com.talex.server.controllers.kyc;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.responses.KycStepResponseDto;
import com.talex.server.services.ekyc.IKycStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bước KYC", description = "API truy vấn và lọc thông tin các bước trong quy trình KYC")
public class KycStepController {
        private final IKycStepService kycStepService;

        @GetMapping("/{kycStepId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Lấy bước KYC theo ID", description = "Trả về chi tiết bước KYC theo ID cho người dùng đã đăng nhập.")
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
        @Operation(summary = "Lọc các bước KYC", description = "Lọc danh sách bước KYC theo tham số truy vấn để hiển thị hoặc xử lý.")
        public ResponseEntity<BaseResponse> filterKycSteps(
                        @RequestParam Map<String, Object> params) {
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
