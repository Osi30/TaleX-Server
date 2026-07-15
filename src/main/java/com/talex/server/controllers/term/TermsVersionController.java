package com.talex.server.controllers.term;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.terms.TermsVersionRequestDto;
import com.talex.server.dtos.requests.filters.TermVersionFilterRequestDto;
import com.talex.server.dtos.responses.creator.TermsVersionResponseDto;
import com.talex.server.enums.TermsType;
import com.talex.server.services.terms.ITermsVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/terms-versions")
@RequiredArgsConstructor
@Tag(name = "Term Versions", description = "API quản lý phiên bản điều khoản và nội dung điều khoản đang hoạt động")
public class TermsVersionController {
        private final ITermsVersionService termsService;

        @PostMapping
//        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Tạo phiên bản điều khoản mới", description = "Tạo bản ghi phiên bản điều khoản mới, chỉ admin được phép.")
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
        @Operation(summary = "Lấy phiên bản điều khoản theo ID", description = "Trả về thông tin phiên bản điều khoản theo ID.")
        public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
                TermsVersionResponseDto resp = termsService.getById(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @GetMapping()
        @Operation(summary = "Tìm kiếm phiên bản điều khoản", description = "Tìm kiếm và phân trang các phiên bản điều khoản theo tiêu chí và loại.")
        public ResponseEntity<BaseResponse> search(
                        @RequestParam(required = false) Map<String, Object> criteria,
                        @RequestParam(required = false) String[] types,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String sortDirection,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer pageSize) {

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
        @Operation(summary = "Lấy điều khoản đang hoạt động", description = "Trả về phiên bản điều khoản đang được kích hoạt theo loại.")
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
        @Operation(summary = "Cập nhật phiên bản điều khoản", description = "Cập nhật nội dung phiên bản điều khoản theo ID.")
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
        @Operation(summary = "Xóa phiên bản điều khoản", description = "Xóa phiên bản điều khoản theo ID, chỉ admin được phép.")
        public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
                termsService.delete(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Deleted")
                                .data(null)
                                .build());
        }
}
