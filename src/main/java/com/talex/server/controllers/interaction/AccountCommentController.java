package com.talex.server.controllers.interaction;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.interaction.request.CommentUpdateRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import com.talex.server.services.interaction.IAccountCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
            @Valid @RequestBody CommentUpdateRequest request) {
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

    @Operation(summary = "Ẩn bình luận", description = "Ẩn bình luận gốc dưới một tập phim.")
    @PatchMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<BaseResponse> hideComment(
            @PathVariable String commentId
    ) {
        commentService.hideCommentByAdmin(commentId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Thành công!")
                .build());
    }

    @Operation(
            summary = "Lấy danh sách bình luận gốc của tập phim (Infinite Scroll)",
            description = "Trả về một Slice dữ liệu bình luận cấp cao nhất (không có parent). Càng kéo xuống càng tăng page lên."
    )
    @GetMapping("/episodes/{episodeId}/comments")
    public ResponseEntity<BaseResponse> getTopLevelComments(
            @PathVariable String episodeId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<CommentResponse> comments = commentService.getTopLevelComments(episodeId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách bình luận thành công.")
                .data(comments)
                .build());
    }

    @Operation(
            summary = "Lấy danh sách phản hồi của một bình luận (Nút bấm Xem thêm)",
            description = "Trả về một Slice chứa các bình luận con (reply) khi người dùng chủ động click nút 'Xem thêm phản hồi'."
    )
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<BaseResponse> getCommentReplies(
            @PathVariable String commentId,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Slice<CommentResponse> replies = commentService.getCommentReplies(commentId, pageable);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách phản hồi thành công.")
                .data(replies)
                .build());
    }
}