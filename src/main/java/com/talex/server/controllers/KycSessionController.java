package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.KycSessionFilterRequestDto;
import com.talex.server.dtos.requests.KycSessionRequestDto;
import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.services.IKycSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/kyc-sessions")
@RequiredArgsConstructor
public class KycSessionController {
    private final IKycSessionService kycSessionService;

    @PostMapping
    public ResponseEntity<BaseResponse> createSession() {
        KycSessionResponseDto responseDto = kycSessionService.createSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.builder()
                        .code(201)
                        .message("Tạo phiên KYC thành công!")
                        .data(responseDto)
                        .build());
    }

    @GetMapping("/{kycSessionId}")
    public ResponseEntity<BaseResponse> getSessionById(
            @PathVariable String kycSessionId) {
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
            @RequestParam(defaultValue = "20") Integer pageSize)
    {
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
            @RequestBody KycSessionRequestDto requestDto) {
        KycSessionResponseDto responseDto = kycSessionService.updateSession(kycSessionId, requestDto);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Cập nhật phiên KYC thành công!")
                        .data(responseDto)
                        .build());
    }
}
