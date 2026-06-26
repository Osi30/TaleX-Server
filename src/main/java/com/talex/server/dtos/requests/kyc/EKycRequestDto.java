package com.talex.server.dtos.requests.kyc;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EKycRequestDto {
    @NotNull(message = "Ảnh mặt trước CCCD/CMND là bắt buộc.")
    private MultipartFile idFrontImage;

    @NotNull(message = "Ảnh mặt sau CCCD/CMND là bắt buộc.")
    private MultipartFile idBackImage;

    @NotNull(message = "Ảnh chân dung (selfie) là bắt buộc.")
    private MultipartFile selfieImage;

    @NotNull(message = "Video xác minh là bắt buộc.")
    private MultipartFile livenessVideo;
}
