package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// awsLabel KHÔNG có mặt ở đây (cố ý) — đây là key tra cứu thật từ AWS Rekognition, sửa được
// sẽ làm nhãn gốc "mất tích" khỏi tra cứu thực tế mà không có cảnh báo gì. Chỉ cho sửa phần
// hiển thị (vietnameseText/categoryId); tạo mới nhãn AWS mới thì dùng CreateRequestDto.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLabelTranslationUpdateRequestDto {

    @NotBlank
    private String vietnameseText;

    private UUID categoryId;
}
