package com.talex.server.dtos.interaction.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentUpdateRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
}
