package com.talex.server.controllers.term;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.terms.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.creator.CreatorTermsLogResponseDto;
import com.talex.server.services.terms.ITermsLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Term Logs", description = "API quản lý bản ghi sự kiện người dùng chấp nhận điều khoản")
public class TermsLogController {
        private final ITermsLogService service;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Tạo bản ghi điều khoản", description = "Ghi nhận sự kiện người dùng chấp nhận hoặc cập nhật điều khoản.")
        public ResponseEntity<BaseResponse> create(
                        @CurrentAccountId UUID accountId,
                        @Valid @RequestBody CreatorTermsLogRequestDto dto) {

                service.create(accountId, dto);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.builder()
                                                .code(201)
                                                .message("Created")
                                                .data(null)
                                                .build());
        }

        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Lấy bản ghi điều khoản theo ID", description = "Trả về chi tiết bản ghi chấp nhận điều khoản theo ID.")
        public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
                CreatorTermsLogResponseDto resp = service.getById(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Lấy danh sách bản ghi điều khoản", description = "Lấy danh sách các bản ghi điều khoản của tài khoản đang đăng nhập.")
        public ResponseEntity<BaseResponse> listByAccount(
                        @CurrentAccountId UUID accountId) {
                List<?> list = service.listByAccount(accountId.toString());
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(list)
                                .build());
        }
}
