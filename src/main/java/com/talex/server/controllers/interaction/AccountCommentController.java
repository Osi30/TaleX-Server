package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import com.talex.server.services.interaction.IAccountCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Comments", description = "API ghi nhận bình luận nội dung của người dùng")
public class AccountCommentController {
    private final IAccountCommentService commentService;

    @Operation(summary = "Ghi nhận bình luận", description = "Viết bình luận gốc hoặc phản hồi bình luận dưới một tập phim.")
    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> createComment(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse response = commentService.createComment(accountId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(response)
                .build());
    }

    @Operation(summary = "Chỉnh sửa bình luận", description = "Chỉnh sửa bình luận gốc dưới một tập phim.")
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> updateComment(
            @PathVariable String commentId,
            @CurrentAccountId UUID accountId,
            @RequestBody CommentRequest request) {
        CommentResponse response = commentService.updateComment(accountId, commentId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(response)
                .build());
    }

    @Operation(summary = "Xóa bình luận", description = "Xóa bình luận gốc dưới một tập phim.")
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> deleteComment(
            @CurrentAccountId UUID accountId,
            @PathVariable String commentId
    ) {
        commentService.deleteCommentByOwner(accountId, commentId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Xóa thành công!")
                .build());
    }

    @Operation(summary = "Ẩn hoặc bỏ ẩn bình luận", description = "Ẩn hoặc bỏ ẩn bình luận gốc dưới một tập phim.")
    @PatchMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> hideComment(
            @PathVariable String commentId,
            @RequestBody Boolean hide
    ) {
        commentService.hideCommentByAdmin(commentId, hide);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Thành công!")
                .build());
    }
}