package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.response.AccountLikeResponse;
import com.talex.server.dtos.interaction.response.EpisodeLikeResponse;
import com.talex.server.services.interaction.IAccountLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Likes", description = "API quản lý và truy vấn lượt thích (Like/Unlike) của người dùng")
public class AccountLikeController {

    private final IAccountLikeService accountLikeService;

    @Operation(summary = "Yêu thích tập phim (Like)", description = "Ghi nhận một lượt thích từ người dùng hiện tại đối với tập phim chỉ định.")
    @PostMapping("/episodes/{episodeId}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> likeEpisode(
            @PathVariable String episodeId,
            @CurrentAccountId UUID accountId
    ) {
        accountLikeService.likeEpisode(accountId, episodeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.builder()
                        .code(201)
                        .message("Thích tập phim thành công.")
                        .build()
        );
    }

    @Operation(summary = "Bỏ yêu thích tập phim (Unlike)", description = "Xóa bỏ lượt thích của người dùng hiện tại đối với tập phim chỉ định.")
    @DeleteMapping("/episodes/{episodeId}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> unlikeEpisode(
            @PathVariable String episodeId,
            @CurrentAccountId UUID accountId
    ) {
        accountLikeService.unlikeEpisode(accountId, episodeId);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Bỏ thích tập phim thành công.")
                        .build()
        );
    }

    @Operation(
            summary = "Lấy danh sách người dùng đã thích tập phim này",
            description = "Trả về một danh sách phân trang (Slice) chứa thông tin tài khoản (username, avatar) đã thích tập phim."
    )
    @GetMapping("/episodes/{episodeId}/likes")
    public ResponseEntity<BaseResponse> getLikesByEpisode(
            @PathVariable String episodeId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<EpisodeLikeResponse> responses = accountLikeService.getLikesByEpisode(episodeId, pageable);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy danh sách tài khoản thích tập phim thành công.")
                        .data(responses)
                        .build()
        );
    }

    @Operation(
            summary = "Lấy danh sách các tập phim đã thích của người dùng hiện tại",
            description = "Trả về danh sách phân trang (Slice) bao gồm thông tin tập phim và thông tin Series đi kèm mà tài khoản này đã yêu thích."
    )
    @GetMapping("/accounts/me/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getMyLikedEpisodes(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<AccountLikeResponse> responses = accountLikeService.getLikesByAccount(accountId, pageable);
        return ResponseEntity.ok(
                BaseResponse.builder()
                        .code(200)
                        .message("Lấy danh sách tập phim đã thích thành công.")
                        .data(responses)
                        .build()
        );
    }
}