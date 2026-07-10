package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.response.AccountBookmarkResponse;
import com.talex.server.dtos.interaction.response.EpisodeBookmarkResponse;
import com.talex.server.services.interaction.IAccountBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Bookmarks", description = "API quản lý Bookmark tập phim của người dùng")
public class AccountBookmarkController {
    private final IAccountBookmarkService bookmarkService;

    @Operation(summary = "Bookmark tập phim", description = "Lưu tập phim vào danh sách bookmark (Direct Insert).")
    @PostMapping("/episodes/{episodeId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> bookmarkEpisode(
            @CurrentAccountId UUID accountId,
            @PathVariable String episodeId
    ) {
        bookmarkService.bookmarkEpisode(accountId, episodeId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đã lưu tập phim vào danh sách bookmark.")
                .build());
    }

    @Operation(summary = "Hủy Bookmark tập phim", description = "Xóa tập phim ra khỏi danh sách Bookmark.")
    @DeleteMapping("/episodes/{episodeId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> unbookmarkEpisode(
            @CurrentAccountId UUID accountId,
            @PathVariable String episodeId
    ) {
        bookmarkService.unbookmarkEpisode(accountId, episodeId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Đã xóa tập phim khỏi danh sách bookmark.")
                .build());
    }

    @Operation(summary = "Lấy danh sách người dùng đã Bookmark tập phim này")
    @GetMapping("/episodes/{episodeId}/bookmarks")
    public ResponseEntity<BaseResponse> getBookmarksByEpisode(
            @PathVariable String episodeId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<EpisodeBookmarkResponse> bookmarks = bookmarkService.getBookmarksByEpisode(episodeId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách người dùng bookmark thành công.")
                .data(bookmarks)
                .build());
    }

    @Operation(summary = "Lấy danh sách các tập phim mà tài khoản hiện tại đã Bookmark")
    @GetMapping("/bookmarks/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> getMyBookmarks(
            @CurrentAccountId UUID accountId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<AccountBookmarkResponse> bookmarks = bookmarkService.getBookmarksByAccount(accountId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách tập phim bạn đã bookmark thành công.")
                .data(bookmarks)
                .build());
    }
}