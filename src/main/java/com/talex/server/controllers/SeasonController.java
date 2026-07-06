package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.services.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Season", description = "Các API để quản lý các season nằm trong một series, bao gồm tạo, sửa, xóa và thay đổi trạng thái hiển thị.")
public class SeasonController {
    private final SeasonService seasonService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/series/{seriesId}/seasons")
    @Operation(summary = "Tạo season mới cho series", description = "Tạo một season mới thuộc về một series cụ thể. Trạng thái mặc định là DRAFT. Nếu không cung cấp số thứ tự season (seasonNumber), hệ thống sẽ tự động tăng dựa trên các season hiện có. Yêu cầu quyền quản lý series tương ứng.")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String seriesId,
            @Valid @RequestBody SeasonRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Season created",
                        seasonService.create(seriesId, request, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/series/{seriesId}/seasons")
    @Operation(summary = "Lấy danh sách season theo series", description = "Truy xuất danh sách tất cả các season của một series, được sắp xếp tăng dần theo số thứ tự (seasonNumber). Yêu cầu quyền quản lý series đó hoặc quyền Admin/Staff.")
    public ResponseEntity<BaseResponse> listBySeries(
            @PathVariable String seriesId,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                seasonService.listBySeries(seriesId, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/seasons/{id}")
    @Operation(summary = "Lấy chi tiết season", description = "Lấy toàn bộ thông tin của một season cụ thể. Yêu cầu quyền sở hữu nội dung (hoặc Admin/Staff).")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", seasonService.getById(id, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/api/v1/seasons/{id}")
    @Operation(summary = "Cập nhật thông tin season", description = "Cập nhật các trường như số thứ tự (seasonNumber), tiêu đề, mô tả và trạng thái của season. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SeasonRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Season updated",
                seasonService.update(id, request, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/seasons/{id}/hide")
    @Operation(summary = "Ẩn season", description = "Chuyển trạng thái của season sang HIDDEN. Tạm thời ẩn nội dung khỏi công chúng nhưng không xóa dữ liệu. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Season hidden", seasonService.hide(id, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/v1/seasons/{id}/unhide")
    @Operation(summary = "Bỏ ẩn season", description = "Khôi phục trạng thái season từ HIDDEN về lại PUBLISHED. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Season visible", seasonService.unhide(id, accountId.toString())));
    }

    @PatchMapping("/api/v1/seasons/{id}/force-hide")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Ép ẩn season (Admin)")
    public ResponseEntity<BaseResponse> forceHide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Season force-hidden", seasonService.forceHide(id, accountId.toString())));
    }

    @PatchMapping("/api/v1/seasons/{id}/force-unhide")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Bỏ ép ẩn season (Admin)")
    public ResponseEntity<BaseResponse> forceUnhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Season force-unhidden", seasonService.forceUnhide(id, accountId.toString())));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/api/v1/seasons/{id}")
    @Operation(summary = "Xóa season", description = "Thực hiện xóa mềm (soft-delete) season bằng cách đổi trạng thái sang DELETED. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        seasonService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Season deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}