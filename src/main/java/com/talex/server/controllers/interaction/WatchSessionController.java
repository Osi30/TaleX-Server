package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.services.interaction.IWatchSessionService;
import com.talex.server.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Watch Session", description = "API ghi nhận tiến trình xem phim/đọc truyện")
public class WatchSessionController {
    private final IWatchSessionService watchSessionService;

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
}
