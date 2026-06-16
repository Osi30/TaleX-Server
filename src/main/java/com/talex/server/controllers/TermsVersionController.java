package com.talex.server.controllers;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.TermsVersionRequestDto;
import com.talex.server.dtos.requests.filters.TermVersionFilterRequestDto;
import com.talex.server.dtos.responses.TermsVersionResponseDto;
import com.talex.server.enums.TermsType;
import com.talex.server.services.ITermsVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/terms-versions")
@RequiredArgsConstructor
public class TermsVersionController {
        private final ITermsVersionService termsService;

        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<BaseResponse> create(@RequestBody TermsVersionRequestDto dto) {
                TermsVersionResponseDto resp = termsService.create(dto);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.builder()
                                                .code(201)
                                                .message("Created")
                                                .data(resp)
                                                .build());
        }

        @GetMapping("/{id}")
        public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
                TermsVersionResponseDto resp = termsService.getById(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @GetMapping()
        public ResponseEntity<BaseResponse> search(
                @RequestParam(required = false) Map<String, Object> criteria,
                @RequestParam(required = false) String[] types,
                @RequestParam(required = false) String sortBy,
                @RequestParam(required = false) String sortDirection,
                @RequestParam(defaultValue = "1") Integer page,
                @RequestParam(defaultValue = "20") Integer pageSize
        ) {

                BasePageResponse<TermsVersionResponseDto> pageResp = termsService
                        .list(TermVersionFilterRequestDto.builder()
                                .criteria(criteria)
                                .types(types)
                                .sortBy(sortBy)
                                .sortDirection(sortDirection)
                                .page(page)
                                .pageSize(pageSize)
                                .build());
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(pageResp)
                                .build());
        }

        @GetMapping("/active/{type}")
        public ResponseEntity<BaseResponse> getActiveByType(@PathVariable TermsType type) {
                TermsVersionResponseDto resp = termsService.getActiveByType(type);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<BaseResponse> update(
                        @PathVariable String id,
                        @RequestBody TermsVersionRequestDto dto) {
                TermsVersionResponseDto resp = termsService.update(id, dto);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Updated")
                                .data(resp)
                                .build());
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
                termsService.delete(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Deleted")
                                .data(null)
                                .build());
        }
}
