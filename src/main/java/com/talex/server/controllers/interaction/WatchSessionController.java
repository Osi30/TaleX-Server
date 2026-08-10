package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.dtos.interaction.response.WatchSessionResponseDto;
import com.talex.server.services.interaction.WatchSessionService;
import com.talex.server.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Watch Session", description = "API ghi nhận tiến trình xem phim/đọc truyện")
public class WatchSessionController {
    private final WatchSessionService watchSessionService;

    @Operation(
            summary = "Ghi nhận Heartbeat tiến trình xem/đọc",
            description = "Hỗ trợ cả người dùng đã đăng nhập và ẩn danh. Hệ thống tự động bóc tách IP và Account ID (nếu có)."
    )
    @PostMapping("/episodes/watch-progress")
    public ResponseEntity<BaseResponse> recordWatchProgress(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody WatchTimeRequest watchTimeRequest,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtils.getIpAddress(request);
        watchSessionService.sendWatchHeartbeat(watchTimeRequest, accountId, ipAddress);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Heartbeat received.")
                .build());
    }

    @Operation(
            summary = "Lấy danh sách phiên xem gần đây (Watch Sessions)",
            description = "Lấy danh sách các phiên xem có thời gian cập nhật gần nhất của tài khoản theo dạng Slice (phục vụ lướt vô hạn/load thêm)."
    )
    @GetMapping("/watch-sessions/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getRecentWatchSessions(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Slice<WatchSessionResponseDto> response = watchSessionService.getRecentWatchSessions(accountId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách lịch sử xem thành công.")
                .data(response)
                .build());
    }
}
