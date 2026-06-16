package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.services.ITermsLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/terms-logs")
@RequiredArgsConstructor
public class TermsLogController {
    private final ITermsLogService service;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody CreatorTermsLogRequestDto dto
    ) {
        service.create(accountId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Created")
                        .data(null)
                        .build()
                );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorTermsLogResponseDto resp = service.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build()
        );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> listByAccount(
            @CurrentAccountId UUID accountId
    ) {
        List<?> list = service.listByAccount(accountId.toString());
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(list)
                .build()
        );
    }
}
