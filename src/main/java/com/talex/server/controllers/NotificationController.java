package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.notification.NotificationResponseDto;
import com.talex.server.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API quản lý thông báo người dùng")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my-notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách thông báo của tôi", description = "Lấy danh sách thông báo phân trang của người dùng hiện tại.")
    public ResponseEntity<BaseResponse> getMyNotifications(
            @CurrentAccountId UUID accountId,
            @RequestParam(required = false) Map<String, Object> criteria,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<NotificationResponseDto> pageResponse = notificationService.getMyNotifications(
                accountId.toString(),
                BaseFilterRequestDto.builder()
                        .criteria(criteria)
                        .sortBy(sortBy)
                        .sortDirection(sortDirection)
                        .page(page)
                        .pageSize(pageSize)
                        .build()
        );

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đếm số thông báo chưa đọc", description = "Trả về số lượng thông báo chưa đọc.")
    public ResponseEntity<BaseResponse> getUnreadCount(@CurrentAccountId UUID accountId) {
        long count = notificationService.getUnreadCount(accountId.toString());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(count)
                .build());
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đánh dấu đã đọc 1 thông báo", description = "Đổi trạng thái notification thành đã đọc.")
    public ResponseEntity<BaseResponse> markAsRead(
            @PathVariable String notificationId,
            @CurrentAccountId UUID accountId
    ) {
        notificationService.markAsRead(notificationId, accountId.toString());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đã đánh dấu là đã đọc")
                .data(null)
                .build());
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đánh dấu đã đọc tất cả thông báo", description = "Đổi trạng thái toàn bộ thông báo chưa đọc thành đã đọc.")
    public ResponseEntity<BaseResponse> markAllAsRead(@CurrentAccountId UUID accountId) {
        notificationService.markAllAsRead(accountId.toString());

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đã đánh dấu tất cả là đã đọc")
                .data(null)
                .build());
    }
}