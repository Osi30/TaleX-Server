package com.talex.server.controllers;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.services.ITermsLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms-logs")
@RequiredArgsConstructor
public class TermsLogController {
    private final ITermsLogService service;

    @PostMapping
    public ResponseEntity<BaseResponse> create(@RequestBody CreatorTermsLogRequestDto dto) {
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .code(201)
                        .message("Created")
                        .data(null)
                        .build()
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
        CreatorTermsLogResponseDto resp = service.getById(id);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(resp)
                .build()
        );
    }

    @GetMapping()
    public ResponseEntity<BaseResponse> listByAccount(
            @RequestParam(name = "accountId") String accountId
    ) {
        List<?> list = service.listByAccount(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(list)
                .build()
        );
    }
}
