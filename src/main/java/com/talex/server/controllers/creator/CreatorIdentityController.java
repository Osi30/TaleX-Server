package com.talex.server.controllers.creator;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.services.ICreatorIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators/identities")
@RequiredArgsConstructor
@Tag(name = "Creator Identities", description = "API quản lý thông tin định danh của creator")
public class CreatorIdentityController {
        private final ICreatorIdentityService creatorIdentityService;

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
        @Operation(summary = "Lấy danh tính creator theo ID", description = "Trả về thông tin định danh creator theo ID.")
        public ResponseEntity<BaseResponse> getById(@PathVariable String id) {
                CreatorIdentityResponseDto resp = creatorIdentityService.getById(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @GetMapping("/own")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Lấy danh tính creator của tài khoản", description = "Lấy thông tin định danh creator liên kết với tài khoản đang đăng nhập.")
        public ResponseEntity<BaseResponse> getAccountCreatorIdentity(
                        @CurrentAccountId UUID accountId) {
                CreatorIdentityResponseDto resp = creatorIdentityService
                                .getByAccountId(accountId.toString());
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(resp)
                                .build());
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
        @Operation(summary = "Cập nhật danh tính creator", description = "Cập nhật thông tin định danh creator theo ID.")
        public ResponseEntity<BaseResponse> update(
                        @PathVariable String id,
                        @RequestBody CreatorIdentityRequestDto dto) {
                CreatorIdentityResponseDto resp = creatorIdentityService.update(id, dto);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Updated")
                                .data(resp)
                                .build());
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Xóa danh tính creator", description = "Xóa thông tin định danh creator theo ID, chỉ admin được phép.")
        public ResponseEntity<BaseResponse> delete(@PathVariable String id) {
                creatorIdentityService.delete(id);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("Deleted")
                                .data(null)
                                .build());
        }
}
