package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.services.interaction.IAccountShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/episodes/{episodeId}/shares")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> shareEpisode(
            @PathVariable String episodeId,
            @CurrentAccountId UUID accountId
    ) {
        accountShareService.shareEpisode(accountId, episodeId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Ghi nhận hành động chia sẻ tập phim thành công.")
                .build());
    }
}