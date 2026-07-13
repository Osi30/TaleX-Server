package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.ShareRequest;
import com.talex.server.services.interaction.IAccountShareService;
import com.talex.server.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Shares", description = "API quản lý và ghi nhận lượt chia sẻ nội dung tập phim từ người dùng")
public class AccountShareController {
    private final IAccountShareService accountShareService;

    @Operation(
            summary = "Ghi nhận lượt chia sẻ Tập phim",
            description = "Gửi yêu cầu chia sẻ tập phim từ tài khoản hiện tại. Nếu tài khoản đã từng share tập phim này, hệ thống tự động cộng dồn số lần đếm share."
    )
    @PostMapping("/episodes/shares")
    public ResponseEntity<BaseResponse> shareEpisode(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody ShareRequest shareRequest,
            HttpServletRequest request
    ) {
        String ipAddress = RequestUtils.getIpAddress(request);
        shareRequest.setIpAddress(ipAddress);
        shareRequest.setAccountId(accountId);

        accountShareService.shareEpisode(shareRequest);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ghi nhận hành động chia sẻ tập phim thành công.")
                .build());
    }
}