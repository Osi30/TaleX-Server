package com.talex.server.controllers.series;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.series.ContentWarningCategoryCreateRequestDto;
import com.talex.server.dtos.requests.series.ContentWarningCategoryUpdateRequestDto;
import com.talex.server.services.series.ContentWarningCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContentWarningCategoryController {

    private final ContentWarningCategoryService service;

    // Public — Creator cần đọc danh sách nhóm active để hiện checkbox khai báo lúc tạo/sửa
    // series (ageRating=MATURE), không riêng Admin.
    @GetMapping("/api/v1/content-warning-categories")
    public ResponseEntity<BaseResponse> listActive() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách nhóm cảnh báo nội dung thành công")
                .data(service.listActive())
                .build());
    }

    @GetMapping("/api/v1/admin/content-warning-categories")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> listAll() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách nhóm cảnh báo nội dung thành công")
                .data(service.listAll())
                .build());
    }

    @PostMapping("/api/v1/admin/content-warning-categories")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody ContentWarningCategoryCreateRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo nhóm cảnh báo nội dung thành công")
                .data(service.create(accountId, request))
                .build());
    }

    @PutMapping("/api/v1/admin/content-warning-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> update(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody ContentWarningCategoryUpdateRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật nhóm cảnh báo nội dung thành công")
                .data(service.update(accountId, categoryId, request))
                .build());
    }

    @DeleteMapping("/api/v1/admin/content-warning-categories/{categoryId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> delete(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID categoryId
    ) {
        service.delete(accountId, categoryId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xóa nhóm cảnh báo nội dung thành công")
                .build());
    }
}
