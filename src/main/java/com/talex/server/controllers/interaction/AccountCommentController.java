package com.talex.server.controllers.interaction;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.interaction.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import com.talex.server.services.interaction.IAccountCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Comments", description = "API ghi nhận bình luận nội dung của người dùng")
public class AccountCommentController {
    private final IAccountCommentService commentService;

    @Operation(summary = "Ghi nhận bình luận", description = "Viết bình luận gốc hoặc phản hồi bình luận dưới một tập phim.")
    @PostMapping("/episodes/{episodeId}/comments")
    public ResponseEntity<BaseResponse> createComment(
            @PathVariable String episodeId,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse response = commentService.createComment(episodeId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(response)
                .build());
    }

    @Operation(summary = "Chỉnh sửa bình luận", description = "Chỉnh sửa bình luận gốc dưới một tập phim.")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data(response)
                .build());
    }

    @Operation(summary = "Xóa bình luận", description = "Xóa bình luận gốc dưới một tập phim.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Success")
                .data("Xóa thành công!")
                .build());
    }
}