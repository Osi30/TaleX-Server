package com.talex.server.controllers.media;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.media.ViolationLabelTranslationCreateRequestDto;
import com.talex.server.dtos.requests.media.ViolationLabelTranslationUpdateRequestDto;
import com.talex.server.services.media.ViolationLabelTranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ViolationLabelTranslationController {

    private final ViolationLabelTranslationService service;

    // Public — creator/admin FE đều cần đọc để hiển thị nhãn vi phạm tiếng Việt, không phải
    // riêng Admin (khác các endpoint /api/v1/admin/... bên dưới).
    @GetMapping("/api/v1/violation-label-translations")
    public ResponseEntity<BaseResponse> list() {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách bản dịch nhãn vi phạm thành công")
                .data(service.list())
                .build());
    }

    @PostMapping("/api/v1/admin/violation-label-translations")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> create(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody ViolationLabelTranslationCreateRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Tạo bản dịch nhãn vi phạm thành công")
                .data(service.create(accountId, request))
                .build());
    }

    @PutMapping("/api/v1/admin/violation-label-translations/{translationId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> update(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID translationId,
            @Valid @RequestBody ViolationLabelTranslationUpdateRequestDto request
    ) {
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Cập nhật bản dịch nhãn vi phạm thành công")
                .data(service.update(accountId, translationId, request))
                .build());
    }

    @DeleteMapping("/api/v1/admin/violation-label-translations/{translationId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> delete(
            @CurrentAccountId UUID accountId,
            @PathVariable UUID translationId
    ) {
        service.delete(accountId, translationId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Xóa bản dịch nhãn vi phạm thành công")
                .build());
    }
}
