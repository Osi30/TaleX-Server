package com.talex.server.dtos.interaction.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;

    @NotBlank(message = "Phim không được để trống")
    private String episodeId;

    private String commentParentId;
}
