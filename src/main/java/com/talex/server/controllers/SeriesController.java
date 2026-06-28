package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.SeriesRequestDto;
import com.talex.server.services.SeriesService;
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
@RequestMapping("/api/v1/series")
@RequiredArgsConstructor
@Tag(name = "Series", description = "Các API để quản lý nội dung series bao gồm tạo mới, chỉnh sửa, kiểm soát hiển thị và xóa.")
public class SeriesController {
    private final SeriesService seriesService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Tạo series mới", description = "Tạo một series mới với trạng thái DRAFT. Creator sẽ tự động sử dụng profile creator được liên kết. Staff/Admin phải cung cấp creatorId trong request body. Đồng thời API cũng sẽ đồng bộ các category và tag liên quan.")
    public ResponseEntity<BaseResponse> create(
            @Valid @RequestBody SeriesRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response(201, "Series created", seriesService.create(request, accountId.toString())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Operation(summary = "Danh sách tất cả series (Admin/Staff)", description = "Lấy danh sách phân trang tất cả các series đang hoạt động (chưa bị xóa) trên toàn hệ thống.")
    public ResponseEntity<BaseResponse> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ResponseEntity.ok(response(200, "OK", seriesService.list(page, pageSize)));
    }

    @GetMapping("/by-creator")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Danh sách series của creator hiện tại", description = "Lấy danh sách phân trang các series thuộc về profile creator được liên kết với tài khoản đang đăng nhập.")
    public ResponseEntity<BaseResponse> listByCreator(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK",
                seriesService.listByCreator(accountId.toString(), page, pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Lấy chi tiết series theo ID", description = "Lấy toàn bộ thông tin chi tiết của một series cụ thể. Người dùng yêu cầu phải có quyền sở hữu hoặc đủ quyền hạn (role) để xem.")
    public ResponseEntity<BaseResponse> getById(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "OK", seriesService.getById(id, accountId.toString())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Cập nhật series", description = "Cập nhật các trường cho phép thay đổi (tiêu đề, mô tả, hình ảnh, v.v.) và đồng bộ lại category, tag. Yêu cầu quyền sở hữu nội dung hoặc quyền admin.")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String id,
            @Valid @RequestBody SeriesRequestDto request,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series updated",
                seriesService.update(id, request, accountId.toString())));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Xuất bản series", description = "Cập nhật trạng thái series thành PUBLISHED và đặt quyền hiển thị thành PUBLIC. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> publish(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series published", seriesService.publish(id, accountId.toString())));
    }

    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Ẩn series", description = "Cập nhật trạng thái series thành HIDDEN, gỡ series khỏi chế độ xem công khai mà không xóa nó. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> hide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series hidden", seriesService.hide(id, accountId.toString())));
    }

    @PatchMapping("/{id}/unhide")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Bỏ ẩn series", description = "Khôi phục series đang bị ẩn về lại trạng thái PUBLISHED. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> unhide(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        return ResponseEntity.ok(response(200, "Series visible", seriesService.unhide(id, accountId.toString())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CREATOR', 'STAFF', 'ADMIN')")
    @Operation(summary = "Xóa series", description = "Xóa mềm (soft-delete) một series bằng cách chuyển trạng thái thành DELETED và đánh dấu isDeleted là true. Yêu cầu quyền sở hữu nội dung.")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String id,
            @CurrentAccountId UUID accountId) {
        seriesService.delete(id, accountId.toString());
        return ResponseEntity.ok(response(200, "Series deleted", null));
    }

    private BaseResponse response(int code, String message, Object data) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}