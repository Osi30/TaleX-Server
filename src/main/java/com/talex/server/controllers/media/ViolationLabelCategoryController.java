package com.talex.server.controllers.media;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.media.ViolationLabelCategoryRequestDto;
import com.talex.server.services.media.ViolationLabelCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ViolationLabelCategoryController {

    private final ViolationLabelCategoryService service;

    // Public — dropdown chọn nhóm trong form dịch nhãn (cả Admin CRUD lẫn nơi khác nếu cần
    // đọc) đều cần đọc được, không riêng Admin.
    @GetMapping("/api/v1/violation-label-categories")
    public ResponseEntity<BaseResponse> list() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách nhóm nhãn vi phạm thành công")
                .data(service.list())
                .build());
    }

    @PostMapping("/api/v1/admin/violation-label-categories")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody ViolationLabelCategoryRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo nhóm nhãn vi phạm thành công")
                .data(service.create(accountId, request))
                .build());
    }

    @PutMapping("/api/v1/admin/violation-label-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> update(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody ViolationLabelCategoryRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật nhóm nhãn vi phạm thành công")
                .data(service.update(accountId, categoryId, request))
                .build());
    }

    @DeleteMapping("/api/v1/admin/violation-label-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> delete(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID categoryId
    ) {
        service.delete(accountId, categoryId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xóa nhóm nhãn vi phạm thành công")
                .build());
    }
}
