package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.response.AccountFollowInfoDto;
import com.talex.server.dtos.interaction.request.FollowRequestDto;
import com.talex.server.services.interaction.IAccountFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@Tag(name = "Account Follows", description = "API quản lý mối quan hệ tương tác theo dõi giữa các người dùng")
public class AccountFollowController {
    private final IAccountFollowService accountFollowService;

    @Operation(summary = "Theo dõi tài khoản", description = "Ghi nhận hành động một tài khoản bắt đầu theo dõi một tài khoản khác.")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> follow(
            @CurrentAccountId UUID accountId,
            @RequestBody FollowRequestDto request
    ) {
        request.setFollowerId(accountId);
        accountFollowService.follow(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Theo dõi tài khoản thành công.")
                .build());
    }

    @Operation(summary = "Hủy theo dõi tài khoản", description = "Xóa bỏ mối quan hệ theo dõi giữa hai tài khoản ra khỏi hệ thống.")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> unfollow(
            @CurrentAccountId UUID accountId,
            @RequestBody FollowRequestDto request
    ) {
        request.setFollowerId(accountId);
        accountFollowService.unfollow(request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Hủy theo dõi tài khoản thành công.")
                .build());
    }

    @Operation(summary = "Lấy danh sách người theo dõi (Followers)", description = "Lấy danh sách phân trang (cuộn vô hạn) những tài khoản đang theo dõi tài khoản được chỉ định (chỉ lấy tài khoản ACTIVE).")
    @GetMapping("/followers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getFollowers(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Slice<AccountFollowInfoDto> followers = accountFollowService.getFollowers(accountId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách người theo dõi thành công.")
                .data(followers)
                .build());
    }

    @Operation(summary = "Lấy danh sách đang theo dõi (Followed/Following)", description = "Lấy danh sách phân trang (cuộn vô hạn) những tài khoản mà tài khoản được chỉ định đang theo dõi (chỉ lấy tài khoản ACTIVE).")
    @GetMapping("/followed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getFollowed(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Slice<AccountFollowInfoDto> followed = accountFollowService.getFollowed(accountId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách đang theo dõi thành công.")
                .data(followed)
                .build());
    }
}